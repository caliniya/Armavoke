package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.io.*;
import caliniya.armavoke.ui.Button;
import arc.files.Fi;

public class HUDFragment {

  private boolean isCommandEnabled = false;
  
  // 右侧容器，用于切换内容
  private Table rightContainer;
  
  // 两个面板引用
  private Table buildingPanel;
  private Table commandPanel;

  public void build() {
    // 主容器
    Table root = new Table();
    root.setFillParent(true);
    // 设置为只允许子元素响应点击，避免遮挡游戏视野
    root.touchable = Touchable.childrenOnly;
    Core.scene.root.addChild(root);

    // === 1. 左下角：指挥模式开关 ===
    Table leftTable = new Table();
    leftTable.bottom().left();

    Button commandBtn = new Button(
        () -> {
          isCommandEnabled = !isCommandEnabled;
          Events.fire(new EventType.CommandChange(isCommandEnabled));
          updateRightPanel();
        },
        "@command");

    leftTable.add(commandBtn).size(120f, 50f).margin(10f);

    // === 2. 右下角：动态面板容器 ===
    rightContainer = new Table();
    rightContainer.bottom().right();

    // 初始化两个面板
    setupBuildingPanel();
    setupCommandPanel();

    // 默认显示建筑面板
    updateRightPanel();

    // === 布局组合 ===
    root.add(leftTable).expand().bottom().left();
    root.add(rightContainer).expand().bottom().right();
  }

  /** 初始化建筑面板 (默认显示) */
  private void setupBuildingPanel() {
    buildingPanel = new Table();
    
    buildingPanel.add("[lightgray]建筑菜单[]").row();
    buildingPanel.add().height(10f).row();
    
    Table btnRow = new Table();
    btnRow.defaults().size(70f, 50f).pad(5f);
    
    btnRow.button("电力", () -> Log.info("打开电力建筑列表"));
    btnRow.button("生产", () -> Log.info("打开生产建筑列表"));
    btnRow.button("防御", () -> Log.info("打开防御建筑列表"));
    
    buildingPanel.add(btnRow);
  }

  /** 初始化单位指挥面板 (指挥模式开启时显示) */
  private void setupCommandPanel() {
    commandPanel = new Table();
    
    commandPanel.add("[accent]单位指挥[]").row();
    commandPanel.add().height(10f).row();

    // --- 第一行：基础指令 ---
    Table basicRow = new Table();
    basicRow.defaults().size(70f, 50f).pad(5f);
    basicRow.button("攻击", () -> Log.info("攻击指令"));
    basicRow.button("移动", () -> Log.info("移动指令"));
    basicRow.button("防守", () -> Log.info("防守指令"));
    commandPanel.add(basicRow).row();

    // --- 第二行：高级指令 (占位) ---
    Table advRow = new Table();
    advRow.defaults().size(70f, 50f).pad(5f);
    advRow.button("巡逻", () -> Log.info("巡逻指令"));
    advRow.button("技能", () -> Log.info("打开单位技能树"));
    advRow.button("编队", () -> Log.info("编队管理"));
    commandPanel.add(advRow).row();
    
    // --- 第三行：状态显示 (占位) ---
    commandPanel.add("[gray]选中单位状态信息区域[]").padTop(10f);
  }

  /** 更新右侧面板显示内容 */
  private void updateRightPanel() {
    rightContainer.clearChildren();
    
    if (isCommandEnabled) {
      rightContainer.add(commandPanel);
    } else {
      rightContainer.add(buildingPanel);
    }
  }
}