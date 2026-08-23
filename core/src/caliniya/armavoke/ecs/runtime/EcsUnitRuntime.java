package caliniya.armavoke.ecs.runtime;

import arc.math.Angles;
import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.Weapon;
import caliniya.armavoke.type.type.UnitType;

/** Unit simulation operating directly on generated ECS entities. */
public final class EcsUnitRuntime {
  private EcsUnitRuntime() {}

  public static Unit create(TeamTypes team, UnitType type, float x, float y) {
    return EcsEntityFactory.createUnit(type, team, x, y);
  }

  public static void commandMove(Unit unit, float x, float y) {
    if (unit != null && unit.active()) unit.moveTo(x, y);
  }

  public static void commandStop(Unit unit) {
    if (unit != null) unit.stop();
  }

  public static void updateGeneral(Unit unit, float delta) {
    if (unit == null || !unit.active()) return;
    unit.updateBase(delta);
    UnitType type = unit.type();
    if (type != null) type.update(unit, delta);
    Entity target = unit.target();
    boolean canShoot = target != null && target.active() && target.health() > 0f;
    for (Weapon weapon : unit.weapons()) {
      weapon.target = canShoot ? target : null;
      weapon.update(delta, canShoot);
    }
    if (unit.health() <= 0f) unit.kill();
  }

  public static void updateMovement(EcsWorld world, float delta) {
    for (EcsEntity value : world.snapshot()) {
      if (!(value instanceof Unit unit) || !unit.active() || !unit.movementMoving()) continue;
      float tx = unit.movementTargetX();
      float ty = unit.movementTargetY();
      if (unit.runtime().pathIndex < unit.runtime().path.size) {
        arc.math.geom.Point2 point = unit.runtime().path.get(unit.runtime().pathIndex);
        tx = (point.x + 0.5f) * caliniya.armavoke.game.data.WorldData.tilesize;
        ty = (point.y + 0.5f) * caliniya.armavoke.game.data.WorldData.tilesize;
        if (Mathf.dst2(unit.x(), unit.y(), tx, ty) < 16f) unit.runtime().pathIndex++;
      }
      float dst = Mathf.dst(unit.x(), unit.y(), tx, ty);
      if (dst <= Math.max(1f, unit.movementSpeed() * delta)) {
        if (unit.runtime().pathIndex >= unit.runtime().path.size) {
          unit.x(unit.movementTargetX());
          unit.y(unit.movementTargetY());
          unit.stop();
        }
        continue;
      }
      float angle = Angles.angle(unit.x(), unit.y(), tx, ty);
      float vx = Mathf.cosDeg(angle) * unit.movementSpeed();
      float vy = Mathf.sinDeg(angle) * unit.movementSpeed();
      unit.movementVelocityX(vx);
      unit.movementVelocityY(vy);
      unit.x(unit.x() + vx * delta);
      unit.y(unit.y() + vy * delta);
      unit.rotation(angle);
    }
  }

  public static void updateAi(EcsWorld world, float delta) {
    for (EcsEntity value : world.snapshot()) {
      if (!(value instanceof Unit unit) || !unit.active()) continue;
      unit.aiControlThinkTimer(unit.aiControlThinkTimer() + delta);
      Entity target = unit.target();
      if (target == null || !target.active() || target.health() <= 0f) continue;
      UnitType type = unit.type();
      float engage = type == null ? 100f : type.engageRange;
      if (Mathf.dst2(unit.x(), unit.y(), target.x(), target.y()) > engage * engage) {
        unit.moveTo(target.x(), target.y());
      } else {
        unit.stop();
      }
    }
  }
}
