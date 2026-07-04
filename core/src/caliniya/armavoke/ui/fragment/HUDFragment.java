package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.math.Interp;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.game.data.CommandData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.io.*;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.ui.Styles;
import arc.files.Fi;

public class HUDFragment {

  private Table rightContainer;
  private Table buildingPanel;
  private Table commandPanel;

  public void build() {
    Table root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.childrenOnly;
    Core.scene.root.addChild(root);

    // ===== 左上角：宇宙视图按钮 =====
    root.table(top -> {
      top.defaults().pad(12f);

      top.add(new Button("宇宙", () -> {
        if (!UniverseFragment.showing) {
          UniverseFragment.showing = true;
          if (caliniya.armavoke.core.UI.universe != null
              && caliniya.armavoke.core.UI.universe.root != null) {
            caliniya.armavoke.core.UI.universe.root.remove();
          }
          caliniya.armavoke.core.UI.universe = new UniverseFragment();
          caliniya.armavoke.core.UI.universe.build();
        }
      })).size(100f, 44f);
    }).top().left();

    // ===== 左下角：指挥/建筑按钮 =====
    Table leftTable = new Table();
    leftTable.bottom().left();

    Button commandBtn = new Button(
        () -> {
          CommandData.commanding = !CommandData.commanding;
          updateRightPanel();
        },
        "@command");

    leftTable.add(commandBtn).size(120f, 50f).margin(10f);

    // === 右下角：动态面板容器 ===
    rightContainer = new Table();
    rightContainer.bottom().right();

    setupBuildingPanel();
    setupCommandPanel();

    updateRightPanel();

    root.add(leftTable).expand().bottom().left();
    root.add(rightContainer).expand().bottom().right();
  }

  private void setupBuildingPanel() {
    buildingPanel = new Table();
    buildingPanel.background(Styles.background);

    buildingPanel.add("[lightgray]建筑菜单[]").row();
    buildingPanel.add().height(10f).row();

    Table btnRow = new Table();
    btnRow.defaults().size(70f, 50f).pad(5f);

    btnRow.button("电力", () -> Log.info("打开电力建筑列表"));
    btnRow.button("生产", () -> Log.info("打开生产建筑列表"));
    btnRow.button("防御", () -> Log.info("打开防御建筑列表"));

    buildingPanel.add(btnRow);
  }

  private void setupCommandPanel() {
    commandPanel = new Table();
    commandPanel.background(Styles.background);

    commandPanel.add("[light]单位指挥[]").row();
    commandPanel.add().height(10f).row();

    Table basicRow = new Table();
    basicRow.defaults().size(70f, 50f).pad(5f);
    basicRow.button("攻击", () -> Log.info("攻击指令"));
    basicRow.button("移动", () -> Log.info("移动指令"));
    basicRow.button("防守", () -> Log.info("防守指令"));
    commandPanel.add(basicRow).row();

    Table advRow = new Table();
    advRow.defaults().size(70f, 50f).pad(5f);
    advRow.button("巡逻", () -> Log.info("巡逻指令"));
    advRow.button("技能", () -> Log.info("打开单位技能树"));
    advRow.button("编队", () -> Log.info("编队管理"));
    commandPanel.add(advRow).row();

    commandPanel.add("[gray]选中单位状态信息区域[]").padTop(10f);
  }

  private void updateRightPanel() {
    rightContainer.clearChildren();

    Table currentPanel = CommandData.commanding ? commandPanel : buildingPanel;

    currentPanel.clearActions();

    rightContainer.add(currentPanel);

    // --- 动画逻辑 ---
    currentPanel.pack();
    float height = currentPanel.getPrefHeight();

    currentPanel.setTranslation(0, -height);
    currentPanel.addAction(Actions.translateBy(0, height, 0.3f, Interp.fade));
  }
}
