package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.scene.Group;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.core.InitGame;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.ui.*;

import static caliniya.armavoke.base.type.EventType.*;

public class MenuFragment {

  public void build() {
    Table root = new Table();
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
                        Maps.load();
                        UI.Maps();
                      }));
              menu.row();

              menu.add(new Button("test", () -> UI.Window("aaaa",Core.graphics.getWidth()/2,Core.graphics.getHeight()/2)));
              menu.row();

              menu.add(new Button("A3", () -> Log.info("A3")));
              menu.row();

              menu.add(new Button("@exit", () -> Core.app.exit()));
            })
        .width(menuWidth)
        .padLeft(20f)
        .padBottom(60f);
  }
}
