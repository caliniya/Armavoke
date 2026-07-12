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
import caliniya.armavoke.content.Blocks;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.content.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.core.meta.stat.*;
import caliniya.armavoke.game.Unit;
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
                        WorldData.initWorld(400, 400, true);
                        Data.loadSystems();
                        Systems.EP.init();
                        Unit U = UnitTypes.test.create(TeamTypes.Evoke, 100, 100);

                        // 生成随机测试建筑
                        int padding = 0;
                        int buildingCount = 300;
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
                      UI.Game();
                      }));
              menu.row();

              menu.add(
                  new Button(
                      "test3",
                      () -> {
                        Data.load(Core.settings.getDataDirectory().child("map/space.aevs"));
                      }));
              menu.row();

              menu.add(new Button("@exit", () -> Core.app.exit()));
            })
        .width(menuWidth)
        .padLeft(20f)
        .padBottom(60f);
  }
}
