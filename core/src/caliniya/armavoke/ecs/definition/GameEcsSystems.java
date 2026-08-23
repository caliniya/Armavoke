package caliniya.armavoke.ecs.definition;

import caliniya.armavoke.annotations.SystemDef;
import caliniya.armavoke.ecs.generated.access.AiControlAccess;
import caliniya.armavoke.ecs.generated.access.BulletAccess;
import caliniya.armavoke.ecs.generated.access.CollisionAccess;
import caliniya.armavoke.ecs.generated.access.EffectAccess;
import caliniya.armavoke.ecs.generated.access.HealthAccess;
import caliniya.armavoke.ecs.generated.access.MovementAccess;
import caliniya.armavoke.ecs.generated.access.PathfindingAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;
import caliniya.armavoke.ecs.generated.access.ProductionAccess;
import caliniya.armavoke.ecs.generated.access.SpawnerAccess;
import caliniya.armavoke.ecs.generated.access.TargetingAccess;
import caliniya.armavoke.ecs.generated.access.TeamAccess;
import caliniya.armavoke.ecs.runtime.EcsEntity;
import caliniya.armavoke.ecs.runtime.EcsSystem;
import caliniya.armavoke.ecs.runtime.EcsWorld;

public final class GameEcsSystems {
  private GameEcsSystems() {}

  @SystemDef(
      name = "general",
      thread = "main",
      priority = 10,
      reads = {
        GameComponents.Health.class,
        GameComponents.Energy.class,
        GameComponents.Effect.class,
        GameComponents.Production.class,
        GameComponents.Spawner.class
      },
      writes = {
        GameComponents.Health.class,
        GameComponents.Energy.class,
        GameComponents.Effect.class,
        GameComponents.Production.class,
        GameComponents.Spawner.class
      })
  public static final class GeneralSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      for (EcsEntity entity : world.snapshot()) {
        if (entity instanceof HealthAccess health && health.healthHealth() <= 0f) {
          world.remove(entity);
          continue;
        }
        if (entity instanceof EffectAccess effect
            && effect.effectComponent().time >= effect.effectComponent().lifetime) {
          world.remove(entity);
          continue;
        }
        if (entity instanceof ProductionAccess production
            && entity instanceof SpawnerAccess spawner
            && production.productionComponent().crafting
            && production.productionComponent().progress
                >= production.productionComponent().craftTime) {
          production.productionComponent().progress = 0f;
          production.productionComponent().crafting = false;
          spawner.spawnerOutputCount(spawner.spawnerOutputCount() + 1);
        }
      }
    }
  }

  @SystemDef(
      name = "movement",
      thread = "simulation",
      priority = 10,
      reads = {GameComponents.Position.class, GameComponents.Movement.class},
      writes = {GameComponents.Position.class, GameComponents.Movement.class},
      parallel = true)
  public static final class MovementSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      for (EcsEntity entity : world.snapshot()) {
        if (!(entity instanceof PositionAccess position)
            || !(entity instanceof MovementAccess movement)
            || !movement.movementMoving()) continue;
        float dx = movement.movementTargetX() - position.positionX();
        float dy = movement.movementTargetY() - position.positionY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float step = Math.max(0f, movement.movementSpeed()) * delta;
        if (distance <= Math.max(0.001f, step)) {
          position.positionX(movement.movementTargetX());
          position.positionY(movement.movementTargetY());
          movement.movementVelocityX(0f);
          movement.movementVelocityY(0f);
          movement.movementMoving(false);
        } else {
          float velocityX = dx / distance * movement.movementSpeed();
          float velocityY = dy / distance * movement.movementSpeed();
          movement.movementVelocityX(velocityX);
          movement.movementVelocityY(velocityY);
          position.positionX(position.positionX() + velocityX * delta);
          position.positionY(position.positionY() + velocityY * delta);
        }
      }
    }
  }

  @SystemDef(
      name = "bullet",
      thread = "simulation",
      priority = 20,
      reads = {GameComponents.Position.class, GameComponents.Bullet.class},
      writes = {GameComponents.Position.class, GameComponents.Bullet.class})
  public static final class BulletSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      for (EcsEntity entity : world.snapshot()) {
        if (!(entity instanceof PositionAccess position)
            || !(entity instanceof BulletAccess bullet)) continue;
        bullet.bulletTime(bullet.bulletTime() + delta);
        if (bullet.bulletTime() >= bullet.bulletLifetime()) {
          world.remove(entity);
          continue;
        }
        position.positionX(
            position.positionX() + bullet.bulletDirectionX() * bullet.bulletSpeed() * delta);
        position.positionY(
            position.positionY() + bullet.bulletDirectionY() * bullet.bulletSpeed() * delta);
      }
    }
  }

  @SystemDef(
      name = "targeting",
      thread = "simulation",
      priority = 30,
      interval = 2,
      after = "movement",
      reads = {
        GameComponents.Position.class,
        GameComponents.Team.class,
        GameComponents.Health.class,
        GameComponents.Targeting.class
      },
      writes = GameComponents.Targeting.class)
  public static final class TargetingSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      EcsEntity[] snapshot = world.snapshot();
      for (EcsEntity entity : snapshot) {
        if (!(entity instanceof PositionAccess position)
            || !(entity instanceof TeamAccess team)
            || !(entity instanceof TargetingAccess targeting)) continue;
        float timer = targeting.targetingScanTimer() + delta;
        targeting.targetingScanTimer(timer);
        if (timer < targeting.targetingScanInterval()) continue;
        targeting.targetingScanTimer(0f);
        float bestDistance = targeting.targetingRange() * targeting.targetingRange();
        int bestId = -1;
        for (EcsEntity candidate : snapshot) {
          if (candidate == entity
              || !(candidate instanceof PositionAccess otherPosition)
              || !(candidate instanceof TeamAccess otherTeam)
              || !(candidate instanceof HealthAccess health)
              || health.healthHealth() <= 0f
              || team.teamTeamId() == otherTeam.teamTeamId()) continue;
          float dx = otherPosition.positionX() - position.positionX();
          float dy = otherPosition.positionY() - position.positionY();
          float distance = dx * dx + dy * dy;
          if (distance < bestDistance) {
            bestDistance = distance;
            bestId = candidate.id();
          }
        }
        targeting.targetingTargetId(bestId);
      }
    }
  }

  @SystemDef(
      name = "collision",
      thread = "simulation",
      priority = 40,
      after = "movement",
      reads = {GameComponents.Position.class, GameComponents.Collision.class},
      writes = {GameComponents.Position.class, GameComponents.Collision.class})
  public static final class CollisionSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      EcsEntity[] snapshot = world.snapshot();
      for (int i = 0; i < snapshot.length; i++) {
        EcsEntity left = snapshot[i];
        if (!(left instanceof PositionAccess leftPosition)
            || !(left instanceof CollisionAccess leftCollision)
            || !leftCollision.collisionSolid()) continue;
        for (int j = i + 1; j < snapshot.length; j++) {
          EcsEntity right = snapshot[j];
          if (!(right instanceof PositionAccess rightPosition)
              || !(right instanceof CollisionAccess rightCollision)
              || !rightCollision.collisionSolid()) continue;
          float dx = rightPosition.positionX() - leftPosition.positionX();
          float dy = rightPosition.positionY() - leftPosition.positionY();
          float overlapX =
              (leftCollision.collisionWidth() + rightCollision.collisionWidth()) / 2f
                  - Math.abs(dx);
          float overlapY =
              (leftCollision.collisionHeight() + rightCollision.collisionHeight()) / 2f
                  - Math.abs(dy);
          if (overlapX <= 0f || overlapY <= 0f) continue;
          if (overlapX < overlapY) {
            float push = Math.copySign(overlapX / 2f, dx == 0f ? 1f : dx);
            leftPosition.positionX(leftPosition.positionX() - push);
            rightPosition.positionX(rightPosition.positionX() + push);
          } else {
            float push = Math.copySign(overlapY / 2f, dy == 0f ? 1f : dy);
            leftPosition.positionY(leftPosition.positionY() - push);
            rightPosition.positionY(rightPosition.positionY() + push);
          }
        }
      }
    }
  }

  @SystemDef(
      name = "pathfinding",
      thread = "path",
      priority = 10,
      interval = 2,
      reads = {
        GameComponents.Position.class,
        GameComponents.Pathfinding.class,
        GameComponents.Collision.class
      },
      writes = {GameComponents.Pathfinding.class, GameComponents.Movement.class})
  public static final class PathfindingSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      for (EcsEntity entity : world.snapshot()) {
        if (!(entity instanceof PathfindingAccess path)
            || !(entity instanceof MovementAccess movement)
            || !path.pathfindingRepath()) continue;
        movement.movementTargetX(path.pathfindingTargetX());
        movement.movementTargetY(path.pathfindingTargetY());
        movement.movementMoving(true);
        path.pathfindingRepath(false);
      }
    }
  }

  @SystemDef(
      name = "ai",
      thread = "ai",
      priority = 10,
      interval = 3,
      reads = {
        GameComponents.Position.class,
        GameComponents.Targeting.class,
        GameComponents.AiControl.class
      },
      writes = {GameComponents.Movement.class, GameComponents.AiControl.class})
  public static final class AiSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      for (EcsEntity entity : world.snapshot()) {
        if (!(entity instanceof AiControlAccess ai)
            || !(entity instanceof TargetingAccess targeting)
            || !(entity instanceof MovementAccess movement)) continue;
        ai.aiControlThinkTimer(ai.aiControlThinkTimer() + delta);
        EcsEntity target = world.find(targeting.targetingTargetId());
        if (target instanceof PositionAccess position && ai.aiControlState() != 2) {
          movement.movementTargetX(position.positionX());
          movement.movementTargetY(position.positionY());
          movement.movementMoving(true);
        }
      }
    }
  }
}
