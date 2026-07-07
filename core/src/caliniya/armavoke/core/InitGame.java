package caliniya.armavoke.core;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.Rand;
import arc.util.Log;
import caliniya.armavoke.Armavoke;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.content.Blocks;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.system.game.GameProcess;
import caliniya.armavoke.system.input.UniverseInput;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.world.*;
import caliniya.armavoke.type.Weapon;
import caliniya.armavoke.system.*;

import static caliniya.armavoke.game.data.WorldData.*;

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
    Systems.UV = new UniverseRender().init();
    Systems.UI = new UniverseInput().init();
    Armavoke.addSystem(
        Systems.MR,
        Systems.UR,
        Systems.GP,
        Systems.BR,
        Systems.UV,
        Systems.UI,
        new Render().init());
    Systems.BP = new BulletProcess().init();
    Systems.UM = new UnitMath().init();
    Systems.EP = new EntityProces().init();

    Unit U = UnitTypes.test.create(TeamTypes.Evoke, 100, 100);

    // 生成随机测试建筑
    int padding = 5;
    int buildingCount = 3;
    for (int i = 0; i < buildingCount; i++) {
      int bx = padding + (int) (Math.random() * (world.W - padding * 2));
      int by = padding + (int) (Math.random() * (world.H - padding * 2));
      if (world.isSolid(bx, by)) {
        i--;
        continue;
      }
      world.setBuilding(bx, by, Blocks.TestBlock, TeamTypes.Mutex);
    }

    // --- 新增：在地图中心生成敌方测试炮塔 ---
    int centerX = world.W / 2;
    int centerY = world.H / 2;

    Building enemyTurret = world.setBuilding(centerX, centerY, Blocks.testTurret, TeamTypes.Mutex);
  }
}
