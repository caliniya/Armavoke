package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.*;
import arc.util.*;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.core.meta.stat.*;

/**
 * 统计信息展示窗口。按 {@link StatType} 分组显示。
 *
 * <p>先这样了
 */
public class DataWindow extends Window {

  private StatStack stack;

  public DataWindow(StatStack data) {
    super("@statistics");
    stack = data;
  }

  @Override
  public void main(Table t) {
    t.clear();
    t.left();
    if (stack == null) return;

    for (StatType type : StatType.values()) {
      Ar<StatData> items = new Ar<>();
      stack.getByType(
          type,
          s -> {
            if (s != null) items.add(s);
          });

      if (!items.any()) continue;

      // 分组标题
      t.add("[#03ECED]" + type.localizedName + "[]")
          .left()
          .padTop(12)
          .padBottom(4)
          .labelAlign(Align.left);
      t.row();

      // 数据项
      for (StatData item : items) {
        if (item == null) {
          continue;
        }
        t.add(item.data).left().padLeft(16).padBottom(2).align(Align.left);
        t.row();
      }
    }
  }
}
