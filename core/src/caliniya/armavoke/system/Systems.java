package caliniya.armavoke.system;

import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.system.game.*;
import caliniya.armavoke.system.input.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.world.*;

public class Systems {

  public static Ar<caliniya.armavoke.system.System> systems =
      new Ar<caliniya.armavoke.system.System>(false);

  public static BulletProcess BP;
  public static UnitMath UM;
  public static EntityProces EP;
  public static MapRender MR;
  public static UnitRender UR;
  public static Render R;
  public static BlockRender BR;
  public static GameProcess GP;
  public static DebugRender DE;
  public static UniverseRender UV;
  public static caliniya.armavoke.base.effect.Effects FX;
  public static caliniya.armavoke.ecs.runtime.EcsScheduler ECS;

  public static void addSystem(caliniya.armavoke.system.System<?>... newSystems) {
    for (caliniya.armavoke.system.System<?> s : newSystems) {
      if (s != null && !systems.contains(s)) {
        systems.add(s);
      }
    }
    systems.sort();
  }
}
