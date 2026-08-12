package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.StringMap;
import arc.util.Align;
import arc.util.Log;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.content.Stars;
import caliniya.armavoke.campaign.Campaign;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.game.data.ProgressData;
import caliniya.armavoke.io.ProgressIO;
import caliniya.armavoke.io.WorldIO;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.ui.windows.Window;
import caliniya.armavoke.world.stars.StarMap;
import caliniya.armavoke.world.stars.StarNode;

/** */
public class UniverseFragment {

  public Table root;

  public void build() {
    root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.childrenOnly;
    root.align(Align.topLeft);

    Table btnGroup = new Table();
    btnGroup.defaults().width(140f).height(54f).pad(4f);
    btnGroup.left().top().align(Align.topLeft);

    btnGroup.add(new Button("保存星域", () -> saveCurrent()));
    btnGroup.row();
    btnGroup.add(new Button("读档", () -> showLoadList()));
    btnGroup.row();
    btnGroup.add(new Button("进度测试", () -> showProgressTest()));
    btnGroup.row();
    btnGroup.add(new Button("关闭", () -> close()));

    root.add(btnGroup).left().top().align(Align.topLeft).get().align(Align.topLeft);
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

    // 不手动设 w/h，用 Window 的默认最小尺寸（屏幕 3/7）
    Window win = new Window("读档");
    win.build();

    boolean any = false;
    for (Fi file : dir.list()) {
      if (file.extension().equals("aess")) {
        any = true;
        win.main
            .add(
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
            .height(56f)
            .pad(4f);
        win.main.row();
      }
    }

    if (!any) {
      win.main.add("[lightgray]没有星域存档[]").pad(20f).center();
    }
  }

  /** 进度测试窗口：直接往 main 容器构建内容（不传整个 Window），再 build()。 */
  private void showProgressTest() {
    Window win =
        new Window("进度测试") {
          @Override
          public void main(Table main) {
            main.clearChildren();
            // 当前星域
            String star = Campaign.progress().currentStar;
            main.add("[lightgray]当前星域:[] " + (star == null || star.isEmpty() ? "(无)" : star))
                .left()
                .row();
            main.add().height(10f).row();

            // 每个节点/地图一行
            StarMap demo = Stars.demo;
            if (demo == null || demo.nodeSet.size == 0) {
              main.add("[red]没有可用的星域节点（Stars.load 没执行？）[]").row();
            } else {
              for (StarNode node : demo.nodeSet) {
                ProgressData.MapProgress p = Campaign.progress().get(node.name);
                boolean unlocked = p != null && p.unlocked;
                boolean completed = p != null && p.completed;

                String status =
                    (unlocked ? "[green]已解锁[]" : "[red]未解锁[]")
                        + " "
                        + (completed ? "[green]已通关[]" : "[red]未通关[]");

                Table row = new Table();
                row.defaults().pad(3f);
                row.add(node.name).width(110f).left();
                row.add(status).expandX().left();
                row.add(
                        new Button(
                            "解锁",
                            () -> {
                              ProgressData.MapProgress mp =
                                  Campaign.progress().getOrCreate(node.name);
                              mp.unlocked = !mp.unlocked;
                              main(main);
                            }))
                    .size(84f, 46f);
                row.add(
                        new Button(
                            "通关",
                            () -> {
                              ProgressData.MapProgress mp =
                                  Campaign.progress().getOrCreate(node.name);
                              mp.completed = !mp.completed;
                              main(main);
                            }))
                    .size(84f, 46f);
                main.add(row).growX();
                main.row();
              }
            }

            main.add().height(12f).row();

            // 操作按钮
            Table ops = new Table();
            ops.defaults().size(120f, 48f).pad(4f);
            ops.add(
                new Button(
                    "设为demo",
                    () -> {
                      Campaign.progress().currentStar = "demo";
                      main(main);
                    }));
            ops.add(
                new Button(
                    "保存进度",
                    () -> {
                      Campaign.saveProgress();
                      Log.info("Progress saved -> @", ProgressIO.file().absolutePath());
                      main(main);
                    }));
            ops.row();
            ops.add(
                new Button(
                    "加载进度",
                    () -> {
                      Campaign.loadProgress();
                      Log.info("Progress loaded, maps=@", Campaign.progress().maps.size);
                      main(main);
                    }));
            ops.add(
                new Button(
                    "清除进度",
                    () -> {
                      Campaign.progress().currentStar = "";
                      Campaign.progress().maps.clear();
                      main(main);
                    }));
            main.add(ops).growX();
            main.row();

            // 文件信息
            Fi pf = ProgressIO.file();
            main.add(
                    "[gray]进度文件: "
                        + (pf.exists() ? "已存在" : "不存在")
                        + "（"
                        + pf.absolutePath()
                        + "）[]")
                .left()
                .padTop(10f)
                .row();
            main.add("[gray]记录地图数: " + Campaign.progress().maps.size + "[]").left().row();
          }
        };
    win.build();
  }
}
