package caliniya.armavoke.core;


import arc.Core;
import arc.Events;
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

public class InitGame {

  static {
   Events.on(EventType.GameInit.class, evevt -> testinit());
  }

  public static void testinit() {
    Maps.load();
    WorldData.initWorld();
    RouteData.init();
    Armavoke.addSystem(new MapRender().init(),new UnitRender().init(), new GameProcess().init());
    new BulletSystem().init();
    new UnitMath().init();
    new UnitProces().init();
    
    Unit ttt = UnitTypes.test.create(500 , 500);
    Unit ta = UnitTypes.test2.create(1000,1000);
    ta.team = TeamTypes.Veto;
  }
}
