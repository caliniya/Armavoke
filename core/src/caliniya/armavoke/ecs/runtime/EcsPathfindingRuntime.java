package caliniya.armavoke.ecs.runtime;

import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.ecs.generated.access.MovementAccess;
import caliniya.armavoke.ecs.generated.access.PathfindingAccess;
import caliniya.armavoke.game.data.RouteData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Unit;

/** Pathfinding worker implementation, replacing the former UnitMath thread. */
public final class EcsPathfindingRuntime {
  private static final float nodeReachTolerance = 4f;

  private EcsPathfindingRuntime() {}

  public static void update(EcsWorld world, float delta) {
    Ar<Unit> units = new Ar<>();
    if (WorldData.moveunits != null) {
      synchronized (WorldData.moveunits) {
        units.addAll(WorldData.moveunits);
      }
    }
    for (Unit unit : units) updateUnit(unit);

    for (EcsEntity entity : world.snapshot()) {
      if (GameEcsBridge.runtime(entity) != null
          || !(entity instanceof PathfindingAccess path)
          || !(entity instanceof MovementAccess movement)
          || !path.pathfindingRepath()) continue;
      movement.movementTargetXBack(path.pathfindingTargetX());
      movement.movementTargetYBack(path.pathfindingTargetY());
      movement.movementMoving(true);
      path.pathfindingRepath(false);
    }
  }

  private static void updateUnit(Unit unit) {
    if (unit == null || unit.health <= 0f) {
      removeFromNavigation(unit);
      return;
    }
    if (unit.routeVersion != RouteData.version) unit.pathed = false;
    if (!unit.pathed) {
      boolean found = calculatePath(unit);
      unit.pathed = true;
      unit.routeVersion = RouteData.version;
      if (!found) {
        GameEcsBridge.syncFromLegacy(unit);
        return;
      }
    }
    calculateVelocity(unit);
    GameEcsBridge.syncFromLegacy(unit);
  }

  private static boolean calculatePath(Unit unit) {
    int sx = (int) (unit.x / WorldData.TILE_SIZE);
    int sy = (int) (unit.y / WorldData.TILE_SIZE);
    int tx = (int) (unit.targetX / WorldData.TILE_SIZE);
    int ty = (int) (unit.targetY / WorldData.TILE_SIZE);
    Point2 reachable = RouteData.findNearestPassable(tx, ty, 2, 1, 8);
    if (reachable == null) {
      stop(unit);
      return false;
    }
    tx = reachable.x;
    ty = reachable.y;
    unit.targetX = tx * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
    unit.targetY = ty * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
    if (sx != tx || sy != ty) {
      unit.path = RouteData.findPath(sx, sy, tx, ty, 2, 1);
      if (unit.path == null) {
        stop(unit);
        return false;
      }
      if (!unit.path.isEmpty()) unit.path.remove(0);
      unit.pathIndex = 0;
    } else {
      if (unit.path == null) unit.path = new Ar<>();
      else unit.path.clear();
    }
    return true;
  }

  private static void calculateVelocity(Unit unit) {
    if (unit.path == null) return;
    float nextX;
    float nextY;
    boolean finalTarget;
    if (unit.path.isEmpty() || unit.pathIndex >= unit.path.size
        || unit.pathIndex == unit.path.size - 1) {
      nextX = unit.targetX;
      nextY = unit.targetY;
      finalTarget = true;
    } else {
      Point2 node = unit.path.get(unit.pathIndex);
      nextX = node.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
      nextY = node.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
      finalTarget = false;
    }
    float distance = Mathf.dst(unit.x, unit.y, nextX, nextY);
    if (!finalTarget && distance <= unit.speed + nodeReachTolerance) {
      unit.pathIndex++;
      calculateVelocity(unit);
    } else if (finalTarget && distance <= unit.speed) {
      stop(unit);
    } else {
      unit.angle = Angles.angle(unit.x, unit.y, nextX, nextY);
      unit.speedX = Mathf.cosDeg(unit.angle) * unit.speed;
      unit.speedY = Mathf.sinDeg(unit.angle) * unit.speed;
    }
  }

  private static void stop(Unit unit) {
    if (unit == null) return;
    unit.speedX = 0f;
    unit.speedY = 0f;
    unit.path = null;
    removeFromNavigation(unit);
  }

  private static void removeFromNavigation(Unit unit) {
    if (unit == null || WorldData.moveunits == null) return;
    synchronized (WorldData.moveunits) {
      WorldData.moveunits.remove(unit);
    }
  }
}
