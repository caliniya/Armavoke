package caliniya.armavoke.core.meta.stat;

import arc.func.Cons;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import caliniya.armavoke.base.tool.Ar;

/** 表示一组统计数据 */
public class StatStack {

  private ObjectMap<StatType, OrderedMap<Stat, StatData>> data =
      new ObjectMap<StatType, OrderedMap<Stat, StatData>>();

  public StatStack add(Stat stat, float value, StatUnit unit) {
    if (!data.containsKey(stat.type)) {
      data.put(stat.type, new OrderedMap<>());
    }
    data.get(stat.type).put(stat, new StatData(stat, value, unit));
    return this;
  }

  public void getByType(StatType type, Cons<StatData> using) {
    if (!data.containsKey(type)) {
      using.get(null);
      return;
    }
    data.get(type)
        .each(
            (K, V) -> {
              using.get(V);
            });
  }

  public void get(Stat stat, Cons<StatData> using) {
    if (!data.containsKey(stat.type)) {
      data.put(stat.type, new OrderedMap<>());
    }
    using.get(data.get(stat.type).get(stat));
  }

  public void getAll(Cons<StatData> using) {
    data.each(
        (K, V) -> {
          V.each(
              (k, v) -> {
                using.get(v);
              });
        });
  }

  /** 清空所有数据 */
  public void clear() {
    data.clear();
  }
}
