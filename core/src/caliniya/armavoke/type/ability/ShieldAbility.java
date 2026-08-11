package caliniya.armavoke.type.ability;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;

/**
 * 护盾能力。
 *
 * <p>机制：
 *
 * <pre>
 * p = 当前容量 / 最大容量          （当前容量比例）
 * I = p × 最大护盾强度            （当前护盾强度）
 * 实际受伤比例 = 1 / I            （护盾存在时，伤害 × 此比例）
 * </pre>
 *
 * 开启时每秒消耗固定能量并回充；能量不足以维持时自动关闭。
 * 容量为 0 时护盾层不参与计算；破盾的溢出伤害不传递到下一层。
 */
public class ShieldAbility extends Ability {

  /** 最大护盾容量。 */
  public float max;

  /** 当前护盾容量。 */
  public float current;

  /** 最大护盾强度（满盾时的强度，默认 2 → 满盾承受 50% 伤害）。 */
  public float maxStrength = 2f;

  /** 回充速率（每秒恢复的护盾容量）。 */
  public float regen;

  /** 开启时每秒消耗的能量。 */
  public float energyCost;

  /** 护盾对各类伤害的百分比抗性（0~1），索引 = DamageType.ordinal()。 */
  public float[] resist = new float[DamageType.values().length];

  /** 护盾对指定伤害类型的抗性（0~1）。 */
  public float resist(DamageType type) {
    return resist[type.ordinal()];
  }

  /** 开关。 */
  public boolean active = true;

  public ShieldAbility(float max) {
    this.max = max;
    this.current = max;
  }

  @Override
  public void update(Entity e, float dt) {
    if (!active) return;

    float cost = energyCost * dt;
    if (cost > 0 && e.energy < cost) {
      // 能量不足以维持 → 自动关闭
      active = false;
      return;
    }
    e.energy -= cost;
    current = Math.min(max, current + regen * dt);
  }

  @Override
  public float applyDamage(Entity e, float damage, DamageType type, boolean breakShield, boolean bypassShield) {
    // 穿盾：护盾完全不拦截，伤害直接穿过
    if (bypassShield) return damage;
    if (!active || current <= 0) return damage;

    float p = current / max;
    float strength = p * maxStrength;
    // 破盾：无视护盾强度减伤（全伤害扣盾，护盾掉得更快）
    float reduction = breakShield ? 1f : (1f / strength);
    float actual = damage * type.shieldMult * (1f - resist[type.ordinal()]) * reduction;
    current -= actual;
    if (current <= 0) current = 0;

    return 0; // 破盾溢出不传递
  }

  /** 当前护盾容量。 */
  public float capacity() {
    return current;
  }

  /** 最大护盾容量。 */
  public float capacityMax() {
    return max;
  }

  /** 当前容量比例（0 ~ 1），用于血条显示。 */
  public float percent() {
    return max <= 0 ? 0f : current / max;
  }
}
