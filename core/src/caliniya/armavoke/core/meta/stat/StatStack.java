package caliniya.armavoke.core.meta.stat;

import arc.func.Cons;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import caliniya.armavoke.base.tool.Ar;

/** 统计信息堆栈。运行时收集 {@link Stat} + 数值 + {@link StatUnit} 三元组。 */
public class StatStack {

  private ObjectMap<StatType, Ar<StatData>> data = new ObjectMap<StatType , Ar<StatData>>();

  public void add(Stat stat, float value, StatUnit unit) {
    if (!data.containsKey(stat.type)) {
      data.put(stat.type, new Ar<>());
    }
    data.get(stat.type).add(new StatData(stat, value, unit));
  }

  public void getByType(StatType type, Cons<StatData> action) {
    data.get(type)
        .each(
            (V) -> {
              action.get(V);
            });
  }

  /** 清空所有数据 */
  public void clear() {
    data.clear();
  }
}
