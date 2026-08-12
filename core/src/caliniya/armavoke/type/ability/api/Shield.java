package caliniya.armavoke.type.ability.api;

/**
 * 护盾契约：统一单体护盾与空间护盾（力场护盾）的**数据访问**接口。
 *
 * <p>只暴露护盾自身的数据（容量/强度/抗性/回充/耗能/比例）， 行为逻辑（减伤结算、能量消耗、 回充 tick）仍属于能力（{@code Ability}）范畴。
 */
public interface Shield {

  /** 当前护盾容量（关闭时为 0）。 */
  float capacity();

  /** 最大护盾容量。 */
  float capacityMax();

  /** 当前护盾强度（= 当前比例 × 最大强度；满盾时为 {@link #maxStrength()}）。 */
  float strength();

  /** 最大护盾强度（满盾时的强度）。 */
  float maxStrength();

  /** 护盾对各类伤害的百分比抗性（0~1），索引 = {@code DamageType.ordinal()}。 */
  float[] resist();

  /** 护盾回充速率（以秒为单位的设计值）。 */
  float regen();

  /** 护盾耗能速率（以秒为单位的设计值；每帧消耗见能力层 {@code energyUse()}）。 */
  float energyCost();

  /** 当前护盾比例（0 ~ 1）。 */
  float percent();
}
