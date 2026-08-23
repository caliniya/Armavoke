package caliniya.armavoke.ecs.runtime;

import arc.math.geom.Point2;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.data.RouteData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Unit;

/** Path requests and routes stored directly in ECS runtime components. */
public final class EcsPathfindingRuntime {
  private EcsPathfindingRuntime() {}

  public static void update(EcsWorld world, float delta) {
    int tile = WorldData.tilesize;
    for (EcsEntity value : world.snapshot()) {
      if (!(value instanceof Unit unit) || !unit.active() || !unit.pathfindingRepath()) continue;
      int sx = (int) (unit.x() / tile), sy = (int) (unit.y() / tile);
      int tx = (int) (unit.pathfindingTargetX() / tile);
      int ty = (int) (unit.pathfindingTargetY() / tile);
      Point2 passable = RouteData.findNearestPassable(tx, ty, 2, 1, 8);
      if (passable != null) { tx = passable.x; ty = passable.y; }
      Ar<Point2> path = RouteData.findPath(sx, sy, tx, ty, 2, 1);
      unit.runtime().path.clear();
      if (path != null) unit.runtime().path.addAll(path);
      unit.runtime().pathIndex = 0;
      unit.pathfindingRouteVersion(unit.pathfindingRouteVersion() + 1);
      unit.pathfindingRepath(false);
    }
  }
}
