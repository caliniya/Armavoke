package caliniya.armavoke.system;

import caliniya.armavoke.core.*;
import caliniya.armavoke.system.game.*;
import caliniya.armavoke.system.input.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.world.*;
import caliniya.armavoke.base.tool.*;

public class Systems {

  public static Ar<caliniya.armavoke.system.System> systems =
      new Ar<caliniya.armavoke.system.System>(15);

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
  public static UniverseInput UI;

  public static void addSystem(caliniya.armavoke.system.System<?>... newSystems) {
    for (caliniya.armavoke.system.System<?> s : newSystems) {
      if (s != null && !systems.contains(s)) {
        systems.add(s);
      } // TODO: 应不应该重复添加
      // 现在我知道了 不重复
    }
    systems.sort();
  }
}
