package caliniya.armavoke.ecs.runtime;

import arc.func.Cons;
import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.Unit;

/** Allocation-light queries over the authoritative ECS snapshot. */
public final class EcsQueries {
  private EcsQueries() {}

  public static EcsEntity[] snapshot() {
    EcsWorld world = EcsRuntime.world();
    return world == null ? new EcsEntity[0] : world.snapshot();
  }

  public static Ar<Unit> units() {
    Ar<Unit> out = new Ar<>();
    for (EcsEntity value : snapshot()) if (value instanceof Unit unit && unit.active()) out.add(unit);
    return out;
  }

  public static Ar<Building> buildings() {
    Ar<Building> out = new Ar<>();
    for (EcsEntity value : snapshot()) if (value instanceof Building building && building.active()) out.add(building);
    return out;
  }

  public static Ar<Bullet> bullets() {
    Ar<Bullet> out = new Ar<>();
    for (EcsEntity value : snapshot()) if (value instanceof Bullet bullet && bullet.active()) out.add(bullet);
    return out;
  }

  public static void intersectUnits(float x, float y, float width, float height, Cons<Unit> consumer) {
    float right = x + width, top = y + height;
    for (EcsEntity value : snapshot()) {
      if (value instanceof Unit unit && unit.active() && unit.x() >= x && unit.x() <= right
          && unit.y() >= y && unit.y() <= top) consumer.get(unit);
    }
  }

  public static Entity closestEnemy(TeamTypes team, float x, float y, float range) {
    Entity result = null;
    float best = range * range;
    for (EcsEntity value : snapshot()) {
      if (!(value instanceof Entity entity) || !entity.active() || entity.health() <= 0f
          || entity.team() == null || entity.team() == team) continue;
      float dst = Mathf.dst2(x, y, entity.x(), entity.y());
      if (dst < best) { best = dst; result = entity; }
    }
    return result;
  }

  public static int count(Class<?> type) {
    int count = 0;
    for (EcsEntity value : snapshot()) if (type.isInstance(value) && value.active()) count++;
    return count;
  }
}
