package caliniya.armavoke.core.meta.stat;

import arc.Core;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.core.meta.ui.Pal;

/** 一个完整的信息组 */
public class StatStack {

  /** 有序map和有序数组。 */
  private final OrderedMap<StatType, Ar<StatData>> types = new OrderedMap<>();

  public static ObjectMap<StatType, StatData> cache;

  static {
    cache = new ObjectMap<>();
    for (StatType s : StatType.values()) {
      cache.put(s, new StatData(Pal.format(Pal.light, s.localizedName), 0));
    }
  }

  public StatStack() {
    for (StatType s : StatType.values()) {
      types.put(s, new Ar<StatData>());
    }
  }

  public StatStack add(Stat stat, float value) {
    return add(stat, value, stat.unit);
  }

  public StatStack add(Stat stat, float value, StatUnit unit) {
    return add(stat, value, unit, 1, -1f);
  }

  public StatStack add(Stat stat, float value, StatUnit unit, float valueMax) {
    return add(stat, value, unit, 1, valueMax);
  }

  public StatStack add(Stat stat, float value, StatUnit unit, int level) {
    return add(stat, value, unit, level, -1f);
  }

  public StatStack add(Stat stat, float value, StatUnit unit, int level, float valueMax) {
    types.get(stat.type).add(new StatData(stat, value, unit, level, valueMax));
    return this;
  }

  public StatStack add(StatData data) {
    types.get(data.type).add(data);
    return this;
  }

  // 注意一下 我们默认是加在支持组里面
  public StatStack add(String raw) {
    types.get(StatType.function).add(new StatData(raw));
    return this;
  }

  public StatStack add(String raw, int level) {
    types.get(StatType.function).add(new StatData(raw, level));
    return this;
  }

  public StatStack add(String raw, StatType type) {
    types.get(type).add(new StatData(raw));
    return this;
  }

  public StatStack add(String raw, int level, StatType type) {
    types.get(type).add(new StatData(raw, level, type));
    return this;
  }

  /** 按 stat 精确查找，命中则就地更新并返回，未命中则新建插入并返回。 */
  public StatData get(Stat stat, float value, StatUnit unit, int level, float valueMax) {
    Ar<StatData> list = types.get(stat.type);
    for (int i = 0; i < list.size; i++) {
      StatData d = list.get(i);
      if (d.stat == stat) {
        d.set(value, valueMax);
        return d;
      }
    }
    StatData d = new StatData(stat, value, unit, level, valueMax);
    list.add(d);
    return d;
  }

  /**按原始文本精确查找纯文本条目，命中则返回，未命中则新建插入并返回。 */
  public StatData get(String raw, int level, StatType type) {
    Ar<StatData> list = types.get(type);
    for (int i = 0; i < list.size; i++) {
      StatData d = list.get(i);
      if (d.stat == null && d.raw.contains(raw)) return d;
    }
    StatData d = new StatData(raw, level, type);
    list.add(d);
    return d;
  }

  public StatData get(Stat stat, float value) {
    return get(stat, value, stat.unit, 1, -1f);
  }

  public StatData get(Stat stat, float value, StatUnit unit) {
    return get(stat, value, unit, 1, -1f);
  }

  public StatData get(Stat stat, float value, StatUnit unit, float valueMax) {
    return get(stat, value, unit, 1, valueMax);
  }

  public StatData get(Stat stat, float value, StatUnit unit, int level) {
    return get(stat, value, unit, level, -1f);
  }

  public StatData get(String raw) {
    return get(raw, 1, StatType.function);
  }

  public StatData get(String raw, int level) {
    return get(raw, level, StatType.function);
  }

  public StatData get(String raw, StatType type) {
    return get(raw, 1, type);
  }

  // 按照预设分组自动查找匹配
  public StatData find(Stat stat) {
    for (StatData data : types.get(stat.type)) {
      if (data.stat == stat) return data;
    }
    return null;
  }

  // 带有特定匹配
  public StatData find(Stat stat, Object tag) {
    for (StatData data : types.get(stat.type)) {
      if (data.stat == stat || data.tag == tag) return data;
    }
    return null;
  }

  public StatData find(String raw, StatType type) {
    for (StatData data : types.get(type)) {
      if (data.raw.contains(raw)) return data;
    }
    return null;
  }

  public StatData find(String raw, StatType type, Object tag) {
    for (StatData data : types.get(type)) {
      if (data.raw.contains(raw) || data.tag == tag) return data;
    }
    return null;
  }

  /** 清空所有分组内容（保留分组结构，可复用）。 */
  public StatStack clear() {
    types.each((K, V) -> V.clear());
    return this;
  }

  /** 递归提供全部 StatData（含 StatType 标题、无分组空标题、能力标题、参数）。 data 已含缩进，渲染端直接显示。 */
  public void each(Cons<StatData> cons) {
    types.each(
        (K, V) -> {
          if (!V.any()) {
            return;
          }
          cons.get(cache.get(K));
          V.each(d -> d.each(a -> cons.get(a)));
        });
  }
}
