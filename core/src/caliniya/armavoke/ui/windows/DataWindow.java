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

  /** 每层缩进的字符（1 个全角空格 = 1 个汉字宽度）。若 1 层不够醒目，改成 {@code "\u3000\u3000"}。 */
  static final String INDENT = "\u3000\u3000";

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

      // 分组标题（none 组为类型名称/描述等顶部信息，不显示空标题）
      if (type != StatType.none) {
        t.add("   " + "[#03ECED]" + type.localizedName + "[]")
            .left()
            .padTop(12)
            .padBottom(4)
            .labelAlign(Align.left);
        t.row();
      }

      // 无分组项直接显示；带分组（能力名）的先显示标题再缩进参数
      String lastGroup = null;
      for (StatStack.StatEntry e : items) {
        if (e.group == null) {
          // 层级已由 StatStack 计算好：none 组名称/描述 0，普通条目 1，护甲抗性 2
          t.add("   " + indent(e.level) + e.data.data).left().padBottom(2).align(Align.left);
          t.row();
        } else {
          // 去重用内部 groupKey（同能力实例合并，同名多实例各自成块），标题显示用 group（无编号）
          if (!e.groupKey.equals(lastGroup)) {
            // 能力名：层 1
            t.add("   " + indent(1) + "[light]" + e.group + "[]").left().padTop(2).padBottom(2);
            t.row();
            lastGroup = e.groupKey;
          }
          // 能力参数 2 / 护盾抗性 3，层级已在 StatStack 计算
          t.add("   " + indent(e.level) + e.data.data).left().padBottom(1).align(Align.left);
          t.row();
        }
      }
    }
  }

  /** 拼接指定层数的缩进字符。 */
  private static String indent(int level) {
    if (level <= 0) return "";
    StringBuilder sb = new StringBuilder(level);
    for (int i = 0; i < level; i++) sb.append(INDENT);
    return sb.toString();
  }
}
