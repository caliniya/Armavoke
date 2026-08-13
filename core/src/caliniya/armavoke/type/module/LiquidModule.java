package caliniya.armavoke.type.module;

import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.game.Contents;
import caliniya.armavoke.type.type.LiquidType;

/**
 * 液体存储模块：容器式的连续资源存储。
 *
 * <p>液体是"量"（连续体积），采用<b>总容量</b>：所有液体量之和不超过 {@link #capacity}。
 */
public class LiquidModule extends Module {

  /** 总容量（所有液体量之和的上限）。 */
  public float capacity = 100f;

  /** 存储数组，索引 = 液体 ID，值 = 当前量。 */
  public float[] liquids;

  /** 过滤数组，true = 允许存储；null = 允许所有。 */
  public boolean[] filter;

  public LiquidModule(float capacity) {
    this.capacity = capacity;
    int size = Math.max(Contents.totalLiquidCount + 1, 10);
    this.liquids = new float[size];
  }

  /** 设置过滤器，仅允许指定液体类型存入。 */
  public void setFilter(LiquidType... types) {
    filter = new boolean[liquids.length];
    if (types != null) {
      for (LiquidType type : types) {
        if (type != null && type.id < filter.length) filter[type.id] = true;
      }
    }
  }

  public void clearFilter() {
    filter = null;
  }

  public boolean accepts(LiquidType type) {
    return type != null && type.id < liquids.length && (filter == null || filter[type.id]);
  }

  /** 当前所有液体总量。 */
  public float total() {
    float t = 0f;
    for (float v : liquids) t += v;
    return t;
  }

  /** 剩余可存储量。 */
  public float free() {
    return capacity - total();
  }

  /** 尝试添加液体，返回实际添加量。 */
  public float add(LiquidType type, float amount) {
    if (!accepts(type) || amount <= 0) return 0f;
    float space = free();
    if (space <= 0) return 0f;
    float added = Math.min(amount, space);
    liquids[type.id] += added;
    return added;
  }

  /** 尝试移除液体，返回实际移除量。 */
  public float remove(LiquidType type, float amount) {
    if (type == null || amount <= 0 || type.id >= liquids.length) return 0f;
    float removed = Math.min(amount, liquids[type.id]);
    liquids[type.id] -= removed;
    return removed;
  }

  /** 获取指定液体的当前量。 */
  public float get(LiquidType type) {
    return (type == null || type.id >= liquids.length) ? 0f : liquids[type.id];
  }

  @Override
  public void write(Writes w) {
    w.f(capacity);
    if (filter != null) {
      w.bool(true);
      int count = 0;
      for (int i = 1; i < filter.length; i++) if (filter[i]) count++;
      w.s((short) count);
      for (int i = 1; i < filter.length; i++) if (filter[i]) w.i(i);
    } else {
      w.bool(false);
    }
    int count = 0;
    for (int i = 1; i < liquids.length; i++) if (liquids[i] > 0) count++;
    w.s((short) count);
    for (int i = 1; i < liquids.length; i++) {
      if (liquids[i] > 0) {
        w.i(i);
        w.f(liquids[i]);
      }
    }
  }

  @Override
  public void read(Reads r) {
    capacity = r.f();
    if (r.bool()) {
      short filterCount = r.s();
      if (filter == null || filter.length != liquids.length) {
        filter = new boolean[liquids.length];
      } else {
        java.util.Arrays.fill(filter, false);
      }
      for (int i = 0; i < filterCount; i++) {
        int id = r.i();
        if (id < filter.length) filter[id] = true;
      }
    } else {
      filter = null;
    }
    short count = r.s();
    java.util.Arrays.fill(liquids, 0f);
    for (int i = 0; i < count; i++) {
      int id = r.i();
      float amt = r.f();
      if (id < liquids.length) liquids[id] = amt;
    }
  }
}
