package caliniya.armavoke.core.meta.stat;

import arc.func.Cons;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import caliniya.armavoke.base.tool.Ar;

/** 表示一组统计数据 */
public class StatStack {

  /** 一条统计项（可带能力分组名）。 */
  public static class StatEntry {
    public final StatType type;
    public final Stat stat;
    public final String group; // 能力名（null = 分组直属）
    public final StatData data;

    public StatEntry(StatType type, Stat stat, String group, StatData data) {
      this.type = type;
      this.stat = stat;
      this.group = group;
      this.data = data;
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
    entries.add(new StatEntry(stat.type, stat, group, new StatData(stat, value, unit)));
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
