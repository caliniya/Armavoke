package caliniya.armavoke.ecs.definition;

import caliniya.armavoke.annotations.SystemDef;
import caliniya.armavoke.ecs.runtime.EcsBulletRuntime;
import caliniya.armavoke.ecs.runtime.EcsGameRuntime;
import caliniya.armavoke.ecs.runtime.EcsPathfindingRuntime;
import caliniya.armavoke.ecs.runtime.EcsSystem;
import caliniya.armavoke.ecs.runtime.EcsWorld;

/** Declarative system graph. Implementations live in ECS-owned runtime services. */
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
      EcsGameRuntime.updateGeneral(world, delta);
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
      EcsPathfindingRuntime.update(world, delta);
    }
  }

  @SystemDef(
      name = "movement",
      thread = "simulation",
      priority = 10,
      after = "pathfinding",
      reads = {GameComponents.Position.class, GameComponents.Movement.class},
      writes = {GameComponents.Position.class, GameComponents.Movement.class},
      parallel = true)
  public static final class MovementSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      EcsGameRuntime.updateMovement(world, delta);
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
      EcsBulletRuntime.update(world, delta);
    }
  }

  @SystemDef(
      name = "targeting",
      thread = "simulation",
      priority = 30,
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
      EcsGameRuntime.updateTargeting(world, delta);
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
      EcsGameRuntime.updateCollision(world, delta);
    }
  }

  @SystemDef(
      name = "ai",
      thread = "ai",
      priority = 50,
      after = "targeting",
      reads = {
        GameComponents.Position.class,
        GameComponents.Targeting.class,
        GameComponents.AiControl.class
      },
      writes = {GameComponents.Movement.class, GameComponents.AiControl.class})
  public static final class AiSystem implements EcsSystem {
    @Override
    public void update(EcsWorld world, float delta) {
      EcsGameRuntime.updateAi(world, delta);
    }
  }
}
