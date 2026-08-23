package caliniya.armavoke.ecs.runtime;

import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.ecs.generated.access.BuildingAccess;
import caliniya.armavoke.ecs.generated.access.CollisionAccess;
import caliniya.armavoke.ecs.generated.access.EffectAccess;
import caliniya.armavoke.ecs.generated.access.HealthAccess;
import caliniya.armavoke.ecs.generated.access.MovementAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;
import caliniya.armavoke.ecs.generated.access.ProductionAccess;
import caliniya.armavoke.ecs.generated.access.SpawnerAccess;
import caliniya.armavoke.ecs.generated.access.TargetingAccess;
import caliniya.armavoke.ecs.generated.access.TeamAccess;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.Weapon;

/** Content-aware implementations used by the generated ECS system declarations. */
public final class EcsGameRuntime {
  private EcsGameRuntime() {}

  public static void updateGeneral(EcsWorld world, float delta) {
    Ar<Entity> freshKills = new Ar<>();
    EcsBulletRuntime.drainFreshKills(freshKills);
    for (Entity entity : freshKills) {
      if (entity != null && entity.health <= 0f && GameEcsBridge.ecs(entity) != null) {
        entity.kill();
      }
    }

    for (EcsEntity entity : world.snapshot()) {
      Object runtime = GameEcsBridge.runtime(entity);
      if (runtime instanceof Unit unit) {
        if (unit.health <= 0f) {
          unit.kill();
          continue;
        }
        unit.updateEcsGeneral(delta);
        unit.canShoot = true;
        GameEcsBridge.syncFromLegacy(unit);
        continue;
      }
      if (runtime instanceof Building building) {
        if (building.health <= 0f) {
          building.kill();
          GameEcsBridge.unregister(building);
          continue;
        }
        building.update(delta);
        GameEcsBridge.syncFromLegacy(building);
        continue;
      }
      updateGeneratedGeneral(world, entity);
    }
    GameEcsBridge.syncAllFromLegacy();
    EcsPersistence.update(world);
  }

  private static void updateGeneratedGeneral(EcsWorld world, EcsEntity entity) {
    if (entity instanceof HealthAccess health && health.healthHealth() <= 0f) {
      world.remove(entity);
      return;
    }
    if (entity instanceof EffectAccess effect
        && effect.effectComponent().time >= effect.effectComponent().lifetime) {
      world.remove(entity);
      return;
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

  public static void updateMovement(EcsWorld world, float delta) {
    for (EcsEntity entity : world.snapshot()) {
      Object runtime = GameEcsBridge.runtime(entity);
      if (runtime instanceof Unit unit) {
        unit.updateEcsMovement(delta);
        GameEcsBridge.syncFromLegacy(unit);
        continue;
      }
      if (!(entity instanceof PositionAccess position)
          || !(entity instanceof MovementAccess movement)
          || !movement.movementMoving()) continue;
      float dx = movement.movementTargetX() - position.positionX();
      float dy = movement.movementTargetY() - position.positionY();
      float distance = (float) Math.sqrt(dx * dx + dy * dy);
      float step = Math.max(0f, movement.movementSpeed()) * delta;
      if (distance <= Math.max(0.001f, step)) {
        position.positionXBack(movement.movementTargetX());
        position.positionYBack(movement.movementTargetY());
        movement.movementVelocityXBack(0f);
        movement.movementVelocityYBack(0f);
        movement.movementMoving(false);
      } else {
        float velocityX = dx / distance * movement.movementSpeed();
        float velocityY = dy / distance * movement.movementSpeed();
        movement.movementVelocityXBack(velocityX);
        movement.movementVelocityYBack(velocityY);
        position.positionXBack(position.positionX() + velocityX * delta);
        position.positionYBack(position.positionY() + velocityY * delta);
      }
    }
  }

  public static void updateTargeting(EcsWorld world, float delta) {
    EcsEntity[] snapshot = world.snapshot();
    for (EcsEntity entity : snapshot) {
      Object runtime = GameEcsBridge.runtime(entity);
      if (runtime instanceof Unit unit) {
        updateUnitTarget(world, entity, unit, delta);
        continue;
      }
      if (runtime instanceof Building building) {
        if (building.target == null || building.target.health <= 0f) {
          building.target = building.block.findTarget(building);
        }
        GameEcsBridge.syncFromLegacy(building);
        continue;
      }
      updateGeneratedTarget(world, entity, snapshot, delta);
    }
  }

  private static void updateUnitTarget(EcsWorld world, EcsEntity ecs, Unit unit, float delta) {
    boolean canTarget = unit.ai == null || unit.ai.canTarget();
    if (!canTarget) {
      unit.target = null;
    } else {
      if (unit.scanCooldown > 0f) unit.scanCooldown -= delta;
      boolean invalid =
          unit.target == null
              || unit.target.health <= 0f
              || unit.target.team == unit.team
              || Mathf.dst2(unit.x, unit.y, unit.target.x, unit.target.y)
                  > unit.type.scanDistance * unit.type.scanDistance;
      if (invalid) unit.target = null;
      if (unit.target == null && unit.scanCooldown <= 0f) {
        unit.scanCooldown = 15f;
        unit.target = nearestRuntimeEnemy(world, ecs, unit.type.scanDistance);
      }
    }

    for (Weapon weapon : unit.weapons) {
      float wx = unit.x + weapon.type.x;
      float wy = unit.y + weapon.type.y;
      if (!canTarget) {
        weapon.target = null;
      } else if (weapon.rotate) {
        if (weapon.target == null
            || weapon.target.health <= 0f
            || Mathf.dst2(wx, wy, weapon.target.x, weapon.target.y)
                > weapon.type.range * weapon.type.range) {
          weapon.type.findTarget(weapon, wx, wy);
        }
      } else {
        weapon.target = unit.target;
      }
    }
    unit.updateWeapons(delta);
    GameEcsBridge.syncFromLegacy(unit);
  }

  private static Entity nearestRuntimeEnemy(EcsWorld world, EcsEntity source, float radius) {
    if (!(source instanceof PositionAccess sourcePosition)
        || !(source instanceof TeamAccess sourceTeam)) return null;
    Entity result = null;
    float best = radius * radius;
    for (EcsEntity candidate : world.snapshot()) {
      if (candidate == source
          || !(candidate instanceof PositionAccess position)
          || !(candidate instanceof TeamAccess team)
          || !(candidate instanceof HealthAccess health)
          || health.healthHealth() <= 0f
          || team.teamTeamId() == sourceTeam.teamTeamId()) continue;
      Object runtime = GameEcsBridge.runtime(candidate);
      if (!(runtime instanceof Entity legacy)) continue;
      float distance =
          Mathf.dst2(
              sourcePosition.positionX(),
              sourcePosition.positionY(),
              position.positionX(),
              position.positionY());
      if (distance < best) {
        best = distance;
        result = legacy;
      }
    }
    return result;
  }

  private static void updateGeneratedTarget(
      EcsWorld world, EcsEntity entity, EcsEntity[] snapshot, float delta) {
    if (!(entity instanceof PositionAccess position)
        || !(entity instanceof TeamAccess team)
        || !(entity instanceof TargetingAccess targeting)) return;
    float timer = targeting.targetingScanTimer() + delta;
    targeting.targetingScanTimer(timer);
    if (timer < targeting.targetingScanInterval()) return;
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

  public static void updateCollision(EcsWorld world, float delta) {
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
        boolean leftMovable = left instanceof MovementAccess;
        boolean rightMovable = right instanceof MovementAccess;
        if (!leftMovable && !rightMovable) continue;
        float dx = rightPosition.positionXBack() - leftPosition.positionXBack();
        float dy = rightPosition.positionYBack() - leftPosition.positionYBack();
        float overlapX =
            (leftCollision.collisionWidth() + rightCollision.collisionWidth()) / 2f
                - Math.abs(dx);
        float overlapY =
            (leftCollision.collisionHeight() + rightCollision.collisionHeight()) / 2f
                - Math.abs(dy);
        if (overlapX <= 0f || overlapY <= 0f) continue;
        float leftShare = leftMovable ? (rightMovable ? 0.5f : 1f) : 0f;
        float rightShare = rightMovable ? (leftMovable ? 0.5f : 1f) : 0f;
        if (overlapX < overlapY) {
          float direction = Math.copySign(1f, dx == 0f ? 1f : dx);
          leftPosition.positionXBack(
              leftPosition.positionXBack() - overlapX * leftShare * direction);
          rightPosition.positionXBack(
              rightPosition.positionXBack() + overlapX * rightShare * direction);
        } else {
          float direction = Math.copySign(1f, dy == 0f ? 1f : dy);
          leftPosition.positionYBack(
              leftPosition.positionYBack() - overlapY * leftShare * direction);
          rightPosition.positionYBack(
              rightPosition.positionYBack() + overlapY * rightShare * direction);
        }
      }
    }
  }

  public static void updateAi(EcsWorld world, float delta) {
    for (EcsEntity entity : world.snapshot()) {
      Object runtime = GameEcsBridge.runtime(entity);
      if (runtime instanceof Unit unit) {
        unit.updateEcsAi(delta);
        GameEcsBridge.syncFromLegacy(unit);
      } else if (entity instanceof caliniya.armavoke.ecs.generated.access.AiControlAccess ai
          && entity instanceof TargetingAccess targeting
          && entity instanceof MovementAccess movement) {
        ai.aiControlThinkTimer(ai.aiControlThinkTimer() + delta);
        EcsEntity target = world.find(targeting.targetingTargetId());
        if (target instanceof PositionAccess position && ai.aiControlState() != 2) {
          movement.movementTargetXBack(position.positionX());
          movement.movementTargetYBack(position.positionY());
          movement.movementMoving(true);
        }
      }
    }
  }
}
