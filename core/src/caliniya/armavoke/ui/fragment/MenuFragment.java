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
                              // ===== 实弹测试场景（Step3 临时）=====
                              // 玩家单位：配同样的防护，双方对打便于观察
                              Unit player = UnitTypes.test.create(TeamTypes.Evoke, 1000, 1000);
                              player.armor = 500;
                              player.armorMax = 500;
                              player.armorValue = 30;
                              player.energy = 100;
                              player.energyMax = 100;
                              player.energyRegen = 10;
                              ShieldAbility pshield = new ShieldAbility(500);
                              pshield.regen = 20;
                              pshield.energyCost = 5;
                              player.add(pshield);

                              // 敌方单位：护甲 30 + 满盾 500 + 能量池
                              Unit enemy = UnitTypes.test.create(TeamTypes.Mutex, 1200, 1000);
                              enemy.armor = 300;
                              enemy.armorMax = 300;
                              enemy.armorValue = 15;
                              enemy.weapons.clear(); // 只挨打不还手，便于观察
                              enemy.health = 20;
                              enemy.maxHealth = 20;
                              enemy.energy = 100;
                              enemy.energyMax = 100;
                              enemy.energyRegen = 10;
                              ShieldAbility shield = new ShieldAbility(500);
                              shield.regen = 0;
                              shield.energyCost = 5;
                              enemy.add(shield);

                              Log.info(
                                  "[实弹测试] 敌方生成: 盾=@ 甲=@ 血=@ 减伤=@",
                                  shield.current,
                                  enemy.armor,
                                  enemy.health,
                                  enemy.armorValue);
                              Log.info("[实弹测试] 玩家单位(1000,1000) 攻击敌方(1200,1000)，敌方不还手");
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
