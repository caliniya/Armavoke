package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import arc.util.Log;
import caliniya.armavoke.core.InitGame;
import caliniya.armavoke.io.GameIO;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.ui.Button;

// 继承自 OverFragment (即之前的 BaseOverlay)
public class MapsFragment extends OverFragment {

  // 重写父类的抽象方法，构建具体内容
  @Override
  protected void buildContent(Table window) {
    // 确保地图列表是最新的 (在展示界面时加载)
    Maps.load();

    // --- 窗口标题 ---
    window.add(new Label("地图选择")).pad(10f).growX().center().row();
    window.image().color(Color.gray).growX().height(3f).padBottom(10f).row();

    // --- 地图列表容器 ---
    Table listTable = new Table();
    listTable.top();

    if (Maps.maps.isEmpty()) {
      listTable.add(new Label("没有找到地图存档")).pad(20f).color(Color.lightGray);
    } else {
      for (Map map : Maps.maps) {
        addMapRow(listTable, map);
      }
    }

    ScrollPane pane = new ScrollPane(listTable);
    pane.setFadeScrollBars(false);
    pane.setScrollingDisabled(true, false);

    window.add(pane).grow().pad(10f).row();

    // --- 底部按钮 ---
    // 调用基类的 close() 方法
    Button closeBtn = new Button("返回", this::close);
    window.add(closeBtn).size(200f, 60f).margin(10f).bottom();
  }

  private void addMapRow(Table container, Map map) {
    Table row = new Table();
    row.background(Core.atlas.drawable("white")).setColor(0.25f, 0.25f, 0.25f, 1f);

    // 预览图
    if (map.safeTexture() != null) {
      Image image = new Image(map.safeTexture());
      image.setScaling(Scaling.fit);
      row.add(image).size(64f).pad(10f);
    }

    // 文本信息
    Table info = new Table();
    info.left();
    info.add(new Label(map.name()))
        .left()
        .growX()
        .color(map.custom ? Color.orange : Color.white)
        .row();

    String detailText = map.width + "x" + map.height + " | " + map.author();
    if (map.space) detailText += " | [Space]";
    info.add(new Label(detailText)).left().color(Color.lightGray).fontScale(0.9f);

    row.add(info).growX().padLeft(10f);

    // 加载按钮
    Button loadBtn =
        new Button(
            "加载",
            () -> {
              try {
                // 初始化游戏环境
                InitGame.testinit();
                // 加载地图数据
                GameIO.load(map.file);
                // 关闭地图选择窗口
                this.close();
              } catch (Exception e) {
                Log.err("加载地图失败", e);
              }
            });

    row.add(loadBtn).size(100f, 50f).pad(10f);
    container.add(row).growX().padBottom(5f).row();
  }
}