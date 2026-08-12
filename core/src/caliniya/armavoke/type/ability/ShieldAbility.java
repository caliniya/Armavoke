package caliniya.armavoke.type.ability;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.core.meta.stat.Stat;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.core.meta.stat.StatType;
import caliniya.armavoke.core.meta.stat.StatUnit;
import caliniya.armavoke.type.ability.api.Shield;

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
 * 开启时每秒消耗固定能量并回充；能量不足以维持时自动关闭。 容量为 0 时护盾层不参与计算；破盾的溢出伤害不传递到下一层。
 */
public class ShieldAbility extends Ability implements Shield {

  /** 最大护盾容量。 */
  public float max;

  /** 当前护盾容量。 */
  public float current;

  /** 最大护盾强度（满盾时的强度，默认 2 → 满盾承受 50% 伤害）。 */
  public float maxStrength = 2f;

  /** 回充速率（以秒为单位设计）。 */
  public float regen;

  /** 回充速率（每帧 = regen / 60，update 用）。 */
  public float regenFrame;

  /** 开启时每秒消耗的能量（以秒为单|&&位设计）。 */
  public float energyCost;

  /** 开启时每帧消耗的能量（= energyCost / 60，update 用）。 */
  public float energyCostFrame;

  /** 护盾对各类伤害的百分比抗性（0~1），索引 = DamageType.ordinal()。 */
  public float[] resist = new float[DamageType.values().length];

  /** 护盾对指定伤害类型的抗性（0~1）。 */
  public float resist(DamageType type) {
    return resist[type.ordinal()];
  }

  /** 开关。 */
  public boolean active = true;

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    this.active = enabled;
  }

  public ShieldAbility(float max) {
    super("shield");
    this.max = max;
    this.current = max;
    this.toggleable = true;
    syncFrames();
  }

  /** 把"每秒"数值同步到"每帧"（字段可能被外部直接修改，update 时再同步一次）。 */
  private void syncFrames() {
    regenFrame = regen / 60f;
    energyCostFrame = energyCost / 60f;
  }

  @Override
  public float energyUse() {
    return active ? energyCostFrame : 0;
  }

  public void update(Entity e, float dt) {
    if (!active) return;
    syncFrames();
    // 能量扣减由 Entity.updateBase 统一按净回复处理（避免能量条抖动）
    if (energyCostFrame > 0 && e.energy <= 0) {
      // 能量耗尽 → 自动关闭
      active = false;
      return;
    }
    current = Math.min(max, current + regenFrame * dt);
  }

  @Override
  public float applyDamage(
      Entity e, float damage, DamageType type, boolean breakShield, boolean bypassShield) {
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
  @Override
  public float capacity() {
    return active ? current : 0f;
  }

  /** 最大护盾容量。 */
  @Override
  public float capacityMax() {
    return max;
  }

  /** 当前护盾强度（= 当前比例 × 最大强度）。 */
  @Override
  public float strength() {
    return max <= 0f ? 0f : (current / max) * maxStrength;
  }

  /** 最大护盾强度（满盾时的强度）。 */
  @Override
  public float maxStrength() {
    return maxStrength;
  }

  /** 护盾对各类伤害的百分比抗性数组（0~1），索引 = DamageType.ordinal()。 */
  @Override
  public float[] resist() {
    return resist;
  }

  /** 护盾回充速率（每秒设计值）。 */
  @Override
  public float regen() {
    return regen;
  }

  /** 护盾耗能速率（每秒设计值）。 */
  @Override
  public float energyCost() {
    return energyCost;
  }

  /** 当前容量比例（0 ~ 1），用于血条显示。 */
  @Override
  public float percent() {
    return max <= 0 ? 0f : current / max;
  }

  @Override
  public void stats(StatStack stack) {
    stack.add(Stat.shield, max, StatUnit.none, localizedName);
    stack.add(Stat.shieldStrength, maxStrength, StatUnit.percent, localizedName);
    stack.add(Stat.shieldRegen, regen, StatUnit.perSecond, localizedName);
    stack.add(Stat.shieldCost, energyCost, StatUnit.perSecond, localizedName);
    stack.addResists(StatType.function, "stat.shieldResist", resist, localizedName);
  }

  @Override
  public ShieldAbility copy() {
    ShieldAbility a = (ShieldAbility) super.copy();
    a.resist = resist.clone();
    return a;
  }
}
