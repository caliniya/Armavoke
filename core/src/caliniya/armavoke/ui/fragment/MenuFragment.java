package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.scene.Group;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.StringMap;
import arc.util.Log;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.core.InitGame;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.core.meta.stat.*;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.io.GameIO;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.map.Maps;
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
                        InitGame.testinit();
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

              menu.add(new Button("test2", () -> {
                      InitGame.testinit();
                      ObjectMap<String , String> tag = new ObjectMap<String , String>();
                      tag.put("author" , "calinya");
                      tag.put("name" , "spaceTest");
                      tag.put("map" , "0000");
                      GameIO.save(Core.settings.getDataDirectory().child("maps/space.aevs") , new StringMap(tag));
              }));
              menu.row();

              menu.add(
                  new Button(
                      "test3",
                      () -> {
                        Log.info(temp);
                      }));
              menu.row();

              menu.add(new Button("@exit", () -> Core.app.exit()));
            })
        .width(menuWidth)
        .padLeft(20f)
        .padBottom(60f);
  }
}
