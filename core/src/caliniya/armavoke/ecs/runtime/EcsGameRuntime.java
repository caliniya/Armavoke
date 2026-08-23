package caliniya.armavoke.ecs.runtime;

import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.ecs.generated.access.EffectAccess;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.world.blocks.produce.unit.Factory;

/** Gameplay systems over generated ECS entities. */
public final class EcsGameRuntime {
  private EcsGameRuntime() {}

  public static void updateGeneral(EcsWorld world, float delta) {
    for (EcsEntity value : world.snapshot()) {
      if (!value.active()) continue;
      EcsEntityFactory.hydrate(value);
      if (value instanceof Unit unit) {
        EcsUnitRuntime.updateGeneral(unit, delta);
      } else if (value instanceof Building building) {
        building.updateBase(delta);
        if (building.block() != null) {
          building.block().update(building, delta);
          if (building.block() instanceof Factory) EcsFactoryRuntime.update(building, delta);
        }
        if (building.health() <= 0f) building.kill();
      } else if (value instanceof EffectAccess effect) {
        effect.effectTime(effect.effectTime() + delta);
        if (effect.effectTime() >= effect.effectLifetime()) world.remove(value);
      }
    }
    EcsPersistence.update(world);
  }

  public static void updateMovement(EcsWorld world, float delta) {
    EcsUnitRuntime.updateMovement(world, delta);
  }

  public static void updateTargeting(EcsWorld world, float delta) {
    for (EcsEntity value : world.snapshot()) {
      if (!(value instanceof Entity entity) || !entity.active()) continue;
      if (value instanceof Unit unit) {
        unit.targetingScanTimer(unit.targetingScanTimer() + delta);
        Entity target = unit.target();
        if (target == null || !target.active() || target.health() <= 0f
            || Mathf.dst2(unit.x(), unit.y(), target.x(), target.y()) > unit.targetingRange() * unit.targetingRange()) {
          unit.target(EcsQueries.closestEnemy(unit.team(), unit.x(), unit.y(), unit.targetingRange()));
        }
      } else if (value instanceof Building building) {
        building.targetingScanTimer(building.targetingScanTimer() + delta);
        Entity target = building.target();
        if (target == null || !target.active() || target.health() <= 0f) {
          building.target(EcsQueries.closestEnemy(building.team(), building.x(), building.y(), building.targetingRange()));
        }
      }
    }
  }

  public static void updateCollision(EcsWorld world, float delta) {
    EcsEntity[] values = world.snapshot();
    for (int i = 0; i < values.length; i++) {
      if (!(values[i] instanceof Unit a) || !a.active() || !a.collisionSolid()) continue;
      for (int j = i + 1; j < values.length; j++) {
        if (!(values[j] instanceof Unit b) || !b.active() || !b.collisionSolid()) continue;
        float min = (Math.max(a.width(), a.height()) + Math.max(b.width(), b.height())) * 0.5f;
        float dx = b.x() - a.x(), dy = b.y() - a.y();
        float dst2 = dx * dx + dy * dy;
        if (dst2 <= 0.0001f || dst2 >= min * min) continue;
        float dst = (float) Math.sqrt(dst2);
        float push = (min - dst) * 0.5f;
        float nx = dx / dst, ny = dy / dst;
        a.x(a.x() - nx * push);
        a.y(a.y() - ny * push);
        b.x(b.x() + nx * push);
        b.y(b.y() + ny * push);
      }
    }
  }

  public static void updateAi(EcsWorld world, float delta) {
    EcsUnitRuntime.updateAi(world, delta);
  }
}
