package caliniya.armavoke.core.meta.stat;

import arc.Core;
import arc.func.Cons;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.DamageType;

/** 表示一组统计数据 */
public class StatStack {

  /** 一条统计项（可带能力分组名）。 */
  public static class StatEntry {
    public final StatType type;
    public final Stat stat;
    public final String group; // 能力名（null = 分组直属）
    public final StatData data;
    public final int indent; // 缩进层级（0 = 组内普通条目，1 = 抗性等次级条目）

    public StatEntry(StatType type, Stat stat, String group, StatData data, int indent) {
      this.type = type;
      this.stat = stat;
      this.group = group;
      this.data = data;
      this.indent = indent;
    }
  }

  private final Ar<StatEntry> entries = new Ar<>();

  public StatStack add(Stat stat, float value, StatUnit unit) {
    return add(stat, value, unit, null);
  }

  /**
   * @param group 能力名等子组标题（null 则直接显示在分组下）
   */
  public StatStack add(Stat stat, float value, StatUnit unit, String group) {
    entries.add(new StatEntry(stat.type, stat, group, new StatData(stat, value, unit), 0));
    return this;
  }

  /**
   * 添加一条原始文本条目（抗性列表等多行数据用）。
   *
   * @param type 所属统计组
   * @param text 直接展示的文本
   * @param group 能力名等子组标题（null 则直接显示在分组下）
   */
  public StatStack addRaw(StatType type, String text, String group) {
    return addRaw(type, text, group, 0);
  }

  /** 带缩进层级的原始文本条目（indent 1 起为次级条目）。 */
  public StatStack addRaw(StatType type, String text, String group, int indent) {
    entries.add(new StatEntry(type, null, group, new StatData(text), indent));
    return this;
  }

  /**
   * 添加一整个伤害类型抗性列表（如 "对能量抗性: 20%"）。
   *
   * @param type 所属统计组
   * @param key 抗性文本的 bundle key（如 "stat.armorResist"，占位符 {0}=类型名 {1}=百分比）
   * @param resist 抗性数组，索引 = DamageType.ordinal()
   * @param group 能力名等子组标题（null 则直接显示在分组下）
   */
  public StatStack addResists(StatType type, String key, float[] resist, String group) {
    for (DamageType t : DamageType.values()) {
      if (t.ordinal() < resist.length) {
        addRaw(
            type,
            Core.bundle.format(key, t.localizedName, StatUnit.percent.format(resist[t.ordinal()])),
            group,
            1);
      }
    }
    return this;
  }

  /** 复制另一份 StatStack 的所有条目。 */
  public StatStack addAll(StatStack other) {
    for (StatEntry e : other.entries) {
      entries.add(e);
    }
    return this;
  }

  public void getByType(StatType type, Cons<StatData> using) {
    for (StatEntry e : entries) {
      if (e.type == type) using.get(e.data);
    }
  }

  /** 获取指定类型的所有条目（含 group 信息），按添加顺序。 */
  public void getEntries(StatType type, Cons<StatEntry> using) {
    for (StatEntry e : entries) {
      if (e.type == type) using.get(e);
    }
  }

  public void get(Stat stat, Cons<StatData> using) {
    for (StatEntry e : entries) {
      if (e.stat == stat) using.get(e.data);
    }
  }

  public void getAll(Cons<StatData> using) {
    for (StatEntry e : entries) {
      using.get(e.data);
    }
  }

  /** 清空所有数据 */
  public void clear() {
    entries.clear();
  }
}
