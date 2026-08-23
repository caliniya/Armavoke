package caliniya.armavoke.system.world;

import caliniya.armavoke.ecs.runtime.EcsPathfindingRuntime;
import caliniya.armavoke.system.Systems;

/** Compatibility facade. Pathfinding is dispatched by the generated ECS system graph. */
@Deprecated
public class UnitMath extends caliniya.armavoke.system.System<UnitMath> {
  @Override
  public UnitMath init() {
    return super.init(false);
  }

  @Override
  public void update() {
    if (Systems.ECS != null && Systems.ECS.world() != null) {
      EcsPathfindingRuntime.update(Systems.ECS.world(), 1f);
    }
  }
}
