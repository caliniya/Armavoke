package caliniya.armavoke.game;

import arc.func.Cons;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.type.Unit;

/** Stateless query facade retained for gameplay call sites. */
public final class Entities {
  private Entities() {}

  public static Entity closestEnemy(TeamTypes team, float x, float y, float range) {
    return EcsQueries.closestEnemy(team, x, y, range);
  }

  public static void intersectUnits(float x, float y, float width, float height, Cons<Unit> consumer) {
    EcsQueries.intersectUnits(x, y, width, height, consumer);
  }
}
