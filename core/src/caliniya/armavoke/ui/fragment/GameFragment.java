package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.Settings;
import arc.files.Fi;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.io.*;
import caliniya.armavoke.ui.Button;

public class GameFragment {

  private boolean isCommandEnabled = false;
  private Button commandBtn;
  private Button saveBtn, loadBtn;

  public void build() {
    // 主容器，填满屏幕
    Table root = new Table();
    root.setFillParent(true);
    Core.scene.root.addChild(root);

    // --- 左下角：指挥按钮 ---
    Table leftTable = new Table();
    leftTable.bottom().left();
    
    commandBtn = new Button("@command", () -> {
        isCommandEnabled = !isCommandEnabled;
        Events.fire(new EventType.CommandChange(isCommandEnabled));
    });
    leftTable.add(commandBtn).size(120f, 50f).margin(10f);
    
    // --- 右下角：存档测试按钮 ---
    Table rightTable = new Table();
    rightTable.bottom().right();

    // 定义存档文件路径
    Fi saveFile = Core.settings.getDataDirectory().child("save.aes");

    saveBtn = new Button("Save", () -> {
        try {
            // 确保目录存在
            saveFile.parent().mkdirs();
            GameIO.save(saveFile , null);
        } catch (Exception e) {
            Log.err("Save failed", e);
        }
    });

    loadBtn = new Button("Load", () -> {
        if (!saveFile.exists()) {
            Log.warn("No save file found at @", saveFile);
            return;
        }
        try {
            GameIO.load(saveFile);
        } catch (Exception e) {
            Log.err("Load failed", e);
        }
        
    });

    // 添加按钮到右侧表格，并设置间距
    rightTable.add(saveBtn).size(80f, 50f).margin(5f);
    rightTable.row(); // 换行，或者并在同一行
    rightTable.add(loadBtn).size(80f, 50f).margin(5f);

    // 将左右两个子表格添加到根表格中
    // 使用 expand() 和 bottom() 来正确定位
    root.add(leftTable).expand().bottom().left();
    root.add(rightTable).expand().bottom().right();
  }
}