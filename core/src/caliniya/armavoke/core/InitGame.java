package caliniya.armavoke.core;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.Rand;
import arc.util.Log;
import caliniya.armavoke.Armavoke;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.system.game.GameProcess;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.world.*;
import caliniya.armavoke.type.Weapon;
import caliniya.armavoke.system.*;

public class InitGame {

  static {
    Events.on(EventType.GameInit.class, evevt -> testinit());
  }

  public static void testinit() {
    Maps.load();
    WorldData.initWorld();
    RouteData.init();
    Systems.MR = new MapRender().init();
    Systems.UR = new UnitRender().init();
    Systems.BR = new BlockRender().init();
    Systems.GP = new GameProcess().init();
    //Systems.DE = new DebugRender().init();
    Armavoke.addSystem(
        Systems.MR, Systems.UR, Systems.GP, Systems.BR, new Render().init());
    Systems.BP = new BulletProcess().init();
    Systems.UM = new UnitMath().init();
    Systems.UP = new UnitProces().init();

    UnitTypes.test.create(100, 100);
  }
}
