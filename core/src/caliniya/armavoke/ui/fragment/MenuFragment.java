package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.scene.Group;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.core.InitGame;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.core.meta.stat.*;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.ui.*;

import static caliniya.armavoke.base.type.EventType.*;
import caliniya.armavoke.ui.windows.DataWindow;

public class MenuFragment {

  public Table root;

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

              menu.add(new Button("test", () -> UI.Window("aaaa", 0.5f, 0.5f)));
              menu.row();

              menu.add(
                  new Button(
                      "stattest",
                      () -> {
                        UI.Window(
                            0.8f, 0.6f, (new StatStack().add(Stat.health, 1000, StatUnit.none)));
                      }));
              menu.row();

              menu.add(new Button("@exit", () -> Core.app.exit()));
            })
        .width(menuWidth)
        .padLeft(20f)
        .padBottom(60f);
  }
}
