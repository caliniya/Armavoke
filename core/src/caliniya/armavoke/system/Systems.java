package caliniya.armavoke.system;

import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.ecs.runtime.EcsScheduler;
import caliniya.armavoke.system.render.BlockRender;
import caliniya.armavoke.system.render.DebugRender;
import caliniya.armavoke.system.render.MapRender;
import caliniya.armavoke.system.render.UnitRender;
import caliniya.armavoke.system.render.UniverseRender;

/** Runtime systems. Gameplay simulation is owned solely by {@link EcsScheduler}. */
public final class Systems {
  public static Ar<System> systems = new Ar<>();
  public static MapRender MR;
  public static UnitRender UR;
  public static Render R;
  public static BlockRender BR;
  public static DebugRender DE;
  public static UniverseRender UV;
  public static caliniya.armavoke.base.effect.Effects FX;
  public static EcsScheduler ECS;

  private Systems() {}

  public static void addSystem(System<?>... newSystems) {
    systems.addAll(newSystems);
  }
}
