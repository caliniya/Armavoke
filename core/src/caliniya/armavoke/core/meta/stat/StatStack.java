package caliniya.armavoke.core.meta.stat;

import arc.Core;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.DamageType;

/** 一个完整的信息组 */
public class StatStack {

  /** 有序map和有序数组。 */
  private final OrderedMap<StatType, Ar<StatData>> types = new OrderedMap<>();

  public static ObjectMap<StatType, StatData> cache;

  static {
    cache = new ObjectMap<>();
    for (StatType s : StatType.values()) {
      cache.put(s, new StatData(s.localizedName, 0));
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
  public StatStack add(String raw){
    types.get(StatType.function).add(new StatData(raw));
    return this;
  }
  
  public StatStack add(String raw , StatType type){
    types.get(type).add(new StatData(raw));
    return this;
  }

  /** 递归提供全部 StatData（含 StatType 标题、无分组空标题、能力标题、参数）。 data 已含缩进，渲染端直接显示。 */
  public void each(Cons<StatData> cons) {
    types.each(
        (K, V) -> {
          cons.get(cache.get(K));
          V.each(d -> d.each(a -> cons.get(a)));
        });
  }
}
