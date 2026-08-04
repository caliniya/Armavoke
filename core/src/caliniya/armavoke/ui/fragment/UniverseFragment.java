package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.StringMap;
import arc.util.Log;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.io.WorldIO;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.ui.windows.Window;

/** */
public class UniverseFragment {

  public Table root;

  public void build() {
    root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.childrenOnly;

    Table btnGroup = new Table();
    btnGroup.defaults().width(120f).height(50f).pad(4f);

    btnGroup.add(new Button("保存星域", () -> saveCurrent()));
    btnGroup.row();
    btnGroup.add(new Button("读档", () -> showLoadList()));
    btnGroup.row();
    btnGroup.add(new Button("关闭", () -> close()));

    root.add(btnGroup).left().top();

    Core.scene.root.addChild(root);
    Events.fire(EventType.events.EnterUV);
  }

  private void close() {
    root.remove();
    UI.hud.showHUD();
    UI.pauseWindow.window.visible = true;
    UI.pauseWindow.modalOverlay.visible = true;
    Events.fire(EventType.events.ExitUV);
  }

  /** 保存当前星域到 star/ 目录 */
  private void saveCurrent() {
    if (Game.starMap == null) return;

    Fi dir = Core.settings.getDataDirectory().child("star");
    if (!dir.exists()) dir.mkdirs();

    String name = "StarTest";
    StringMap tags = new StringMap();
    tags.put("name", name);
    tags.put("author", "caliniya");
    tags.put("space", "true");
    tags.put("time", String.valueOf(System.currentTimeMillis()));

    WorldIO.save(dir.child(name + ".aess"), Game.starMap, tags);
  }

  /** 打开星域读档列表 */
  private void showLoadList() {
    Fi dir = Core.settings.getDataDirectory().child("star");
    if (!dir.exists()) dir.mkdirs();

    Window win = new Window("读档");
    win.w = 360f;
    win.h = 400f;
    win.build();

    boolean any = false;
    for (Fi file : dir.list()) {
      if (file.extension().equals("aess")) {
        any = true;
        win.main.add(
            new Button(
                file.nameWithoutExtension(),
                () -> {
                  WorldIO.load(
                      file,
                      map -> {
                        if (map != null) {
                          Game.starMap = map;
                          Log.info(file);
                          Log.info("Okay");
                        }
                      });
                  win.remove();
                }))
            .growX()
            .pad(4f);
        win.main.row();
      }
    }

    if (!any) {
      win.main.add("[lightgray]没有星域存档[]").pad(20f).center();
    }
  }
}
