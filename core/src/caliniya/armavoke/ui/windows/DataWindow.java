package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.Table;
import arc.util.Align;
import caliniya.armavoke.base.tool.Ar;
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
      Ar<StatStack.StatEntry> items = new Ar<>();
      stack.getEntries(type, items::add);
      if (items.isEmpty()) continue;

      // 分组标题
      t.add("[#03ECED]" + type.localizedName + "[]")
          .left()
          .padTop(12)
          .padBottom(4)
          .labelAlign(Align.left);
      t.row();

      // 无分组项直接显示；带分组（能力名）的先显示标题再缩进参数
      String lastGroup = null;
      for (StatStack.StatEntry e : items) {
        if (e.group == null) {
          t.add(e.data.data).left().padLeft(16).padBottom(2).align(Align.left);
          t.row();
        } else {
          if (!e.group.equals(lastGroup)) {
            t.add("[light]" + e.group + "[]").left().padLeft(8).padTop(2).padBottom(2);
            t.row();
            lastGroup = e.group;
          }
          t.add(e.data.data).left().padLeft(28).padBottom(1).align(Align.left);
          t.row();
        }
      }
    }
  }
}
