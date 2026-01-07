package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Group;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import caliniya.armavoke.core.InitGame;
import caliniya.armavoke.io.GameIO;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.ui.Button;

public class MapsFragment {

  /** 构建界面并添加到父节点 */
  public void build(Group parent) {
    // 根容器
    Table root = new Table();
    root.setFillParent(true);

    // 标题
    root.add(new Label("地图选择")).pad(20f).row();

    // --- 地图列表容器 ---
    Table listTable = new Table();
    listTable.top(); // 列表项从顶部开始排列

    // 检查是否有地图
    if (Maps.maps.isEmpty()) {
      listTable.add(new Label("没有找到地图存档")).pad(20f).color(Color.gray);
    } else {
      // 遍历所有地图并生成列表项
      for (Map map : Maps.maps) {
        addMapRow(listTable, map, root);
      }
    }

    // --- 滚动窗格 ---
    ScrollPane pane = new ScrollPane(listTable);
    pane.setFadeScrollBars(false); // 常驻滚动条
    pane.setScrollingDisabled(true, false); // 禁止横向滚动

    // 将滚动窗格添加到根容器，占据剩余空间
    root.add(pane).grow().pad(10f).row();

    // --- 底部按钮 ---
    // 添加一个关闭按钮来移除这个界面
    Button closeBtn =
        new Button(
            "返回",
            () -> {
              root.remove();
            });
    root.add(closeBtn).size(200f, 60f).margin(10f).bottom();

    // 添加到场景
    parent.addChild(root);
  }

  /** 辅助方法：添加单行地图信息 */
  private void addMapRow(Table container, Map map, Table rootUI) {
    Table row = new Table();
    // 给每一行加一个背景，区分明显的边界 (可选)
    // row.background("button");

    // 1. 预览图 (如果有)
    if (map.safeTexture() != null) {
      Image image = new Image(map.safeTexture());
      image.setScaling(Scaling.fit);
      row.add(image).size(64f).padRight(10f);
    }

    // 2. 文本信息 (名称、作者、尺寸)
    Table info = new Table();
    info.left();
    // 名称
    info.add(new Label(map.name()))
        .left()
        .growX()
        .color(map.custom ? Color.orange : Color.white)
        .row();
    // 详细信息
    String detailText = map.width + "x" + map.height + " | " + map.author();
    if (map.space) detailText += " | [Space]";

    info.add(new Label(detailText)).left().color(Color.gray);

    // 让信息栏占据中间所有空间
    row.add(info).growX().padLeft(10f);

    // 3. 加载/开始按钮
    // 这里使用你的自定义 Button 类
    Button loadBtn =
        new Button(
            "加载",
            () -> {
              try {
                // 调用之前的 GameIO 进行加载
                // 注意：这里假设 GameIO.load 接受 Fi 对象
                // 另外，加载地图通常意味着开始游戏，可能需要隐藏当前 UI

                // 1. 加载地图
                InitGame.testinit();
                caliniya.armavoke.io.GameIO.load(map.file);

                // 2. 关闭地图选择窗口
                rootUI.remove();

              } catch (Exception e) {
                // 简单的错误处理
                arc.util.Log.err("加载地图失败", e);
              }
            });

    row.add(loadBtn).size(100f, 50f).padLeft(10f);

    // 将这一行添加到列表容器中
    container.add(row).growX().padBottom(10f).row();

    // 添加分割线 (可选)
    container.image().color(Color.darkGray).growX().height(2f).padBottom(10f).row();
  }
}
