package caliniya.armavoke.system.game;

import arc.util.Time;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.ecs.runtime.EcsGameRuntime;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Unit;

/** Compatibility facade. The ECS general system is the sole gameplay update entry point. */
@Deprecated
public class GameProcess extends caliniya.armavoke.system.System<GameProcess> {
  public Ar<Unit> deadUnits;
  public Ar<Building> deadBuildings;
  public Ar<Entity> freshKilled;

  @Override
  public GameProcess init() {
    index = 5;
    deadUnits = new Ar<>();
    deadBuildings = new Ar<>();
    freshKilled = new Ar<>();
    return super.init(false);
  }

  @Override
  public void update() {
    if (Systems.ECS != null && Systems.ECS.world() != null) {
      EcsGameRuntime.updateGeneral(Systems.ECS.world(), Math.min(Time.delta, 4f));
    }
  }
}
