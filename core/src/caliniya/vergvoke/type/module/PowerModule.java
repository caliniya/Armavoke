package caliniya.vergvoke.type.module;

import arc.util.io.Reads;
import arc.util.io.Writes;

/**
 * 电力模块：类似电池，让单位/建筑存储并运输电力（无实体资源）。
 */
public class PowerModule extends Module {

  /** 当前电力。 */
  public float power;

  /** 最大电力容量。 */
  public float powerMax;

  public PowerModule(float powerMax) {
    this.powerMax = powerMax;
  }

  /** 剩余可充入量。 */
  public float free() {
    return powerMax - power;
  }

  /** 尝试充入，返回实际充入量。 */
  public float add(float amount) {
    if (amount <= 0) return 0f;
    float added = Math.min(amount, free());
    power += added;
    return added;
  }

  /** 尝试抽取，返回实际抽取量。 */
  public float remove(float amount) {
    if (amount <= 0) return 0f;
    float removed = Math.min(amount, power);
    power -= removed;
    return removed;
  }

  @Override
  public void write(Writes w) {
    w.f(power);
    w.f(powerMax);
  }

  @Override
  public void read(Reads r) {
    power = r.f();
    powerMax = r.f();
  }
}
