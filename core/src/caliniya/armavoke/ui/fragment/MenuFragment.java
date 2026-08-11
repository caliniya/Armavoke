package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.scene.Group;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.StringMap;
import arc.util.Log;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.content.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.core.meta.stat.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.ability.ShieldAbility;
import caliniya.armavoke.type.ability.ShieldFieldAbility;
import caliniya.armavoke.game.data.RouteData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.io.DataIO;
import caliniya.armavoke.io.GameIO;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.ui.*;

import static caliniya.armavoke.base.type.EventType.*;
import caliniya.armavoke.ui.windows.DataWindow;

public class MenuFragment {

  public Table root;

  public static String temp;

  public void build() {
    root = new Table();
    root.setFillParent(true);
    root.background(null);
    Core.scene.root.addChild(root);

    float menuWidth = 260f;

    root.bottom().left();

    root.table(
            menu -> {
              menu.defaults().width(menuWidth).height(70f).padBottom(0);

              menu.add(
                  new Button(
                      "@start",
                      () -> {
                        // InitGame.testinit();
                        UI.Game();
                      }));
              menu.row();

              menu.add(
                  new Button(
                      "@mapList",
                      () -> {
                        UI.Maps();
                      }));
              menu.row();

              menu.add(
                  new Button(
                      "test2",
                      () -> {
                        WorldData.initWorld(100, 100, true);
                        Data.loadSystems();
                        Systems.EP.init();
                        Unit U = UnitTypes.test.create(TeamTypes.Evoke, 100, 100);

                        // 生成随机测试建筑
                        int padding = 5;
                        int buildingCount = 10;
                        for (int i = 0; i < buildingCount; i++) {
                          int bx =
                              padding + (int) (Math.random() * (WorldData.world.W - padding * 2));
                          int by =
                              padding + (int) (Math.random() * (WorldData.world.H - padding * 2));
                          if (WorldData.world.isSolid(bx, by)) {
                            i--;
                            continue;
                          }
                          WorldData.world.setBuilding(bx, by, Blocks.TestBlock, TeamTypes.Mutex);
                        }

                        // --- 新增：在地图中心生成敌方测试炮塔 ---
                        int centerX = WorldData.world.W / 2;
                        int centerY = WorldData.world.H / 2;

                        Building enemyTurret =
                            WorldData.world.setBuilding(
                                centerX, centerY, Blocks.testTurret, TeamTypes.Mutex);

                        RouteData.init();
                        ObjectMap<String, String> tag = new ObjectMap<String, String>();
                        tag.put("author", "calinya");
                        tag.put("name", "spaceTest");
                        tag.put("map", "0000");
                        DataIO.setSave(
                            Core.settings.getDataDirectory().child("map/space.aevs"),
                            new StringMap(tag));
                        // UI.Game();
                      }));
              menu.row();

              menu.add(
                  new Button(
                      "test3",
                      () -> {
                        Game.team = TeamTypes.Evoke;

                        // 保留原读档；加载完成后再放置实弹测试场景
                        Data.load(
                            Core.settings.getDataDirectory().child("map/space.aevs"),
                            () -> {
                              // ===== 力场测试（力场外 vs 力场内）=====
                              // 玩家单位 A（力场外 1700,1000）：子弹会被敌方力场拦截
                              Unit playerA = UnitTypes.test.create(TeamTypes.Evoke, 1700, 1000);
                              playerA.armor = 500;
                              playerA.armorMax = 500;
                              playerA.armorValue = 30;
                              playerA.energy = 100;
                              playerA.energyMax = 100;
                              playerA.energyRegen = 10;
                              ShieldAbility pa = new ShieldAbility(500);
                              pa.regen = 2;
                              pa.energyCost = 5;
                              playerA.addAbility(pa);

                              // 玩家单位 B（力场内 1050,1000）：子弹不受力场拦截
                              Unit playerB = UnitTypes.test.create(TeamTypes.Evoke, 1050, 1000);
                              playerB.armor = 500;
                              playerB.armorMax = 500;
                              playerB.armorValue = 30;
                              playerB.energy = 100;
                              playerB.energyMax = 100;
                              playerB.energyRegen = 10;
                              ShieldAbility pb = new ShieldAbility(500);
                              pb.regen = 2;
                              pb.energyCost = 5;
                              playerB.addAbility(pb);

                              // 敌方单位：护甲 30 + 满盾 500 + 护盾力场
                              Unit enemy = UnitTypes.test.create(TeamTypes.Mutex, 1200, 1000);
                              enemy.armor = 500;
                              enemy.armorMax = 500;
                              enemy.armorValue = 30;
                              enemy.weapons.clear(); // 只挨打不还手，便于观察
                              enemy.health = 500;
                              enemy.maxHealth = 500;
                              enemy.energy = 100;
                              enemy.energyMax = 100;
                              enemy.energyRegen = 10;
                              /*
                              ShieldAbility shield = new ShieldAbility(500);
                              shield.regen = 20;
                              shield.energyCost = 5;
                              enemy.add(shield);*/

                              // 护盾力场：正六边形，半径 180，拦截进入的子弹
                              ShieldFieldAbility field = new ShieldFieldAbility(500, 180);
                              field.regen = 5;
                              field.cost = 3;
                              enemy.addAbility(field);

                              Log.info(
                                  "[力场测试] 敌方(1200,1000) 力场半径180；A(1700,1000)力场外、B(1050,1000)力场内");
                            });
                      }));
              menu.row();

              menu.add(new Button("@exit", () -> Core.app.exit()));
            })
        .width(menuWidth)
        .padLeft(20f)
        .padBottom(60f);
  }
}
