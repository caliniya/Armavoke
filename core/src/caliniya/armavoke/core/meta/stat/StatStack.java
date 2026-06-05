package caliniya.armavoke.core.meta.stat;

import caliniya.armavoke.base.tool.Ar;

/** 统计信息堆栈，收集并暴露一组 {@link StatData}。 */
public class StatStack {
  public final Ar<StatData> data = new Ar<>();

  public void add(StatData data) {
    this.data.add(data);
  }
}
