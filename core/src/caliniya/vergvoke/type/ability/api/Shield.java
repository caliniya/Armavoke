package caliniya.vergvoke.type.ability.api;

/**
 * 护盾接口
 *
 * <p> 通用护盾接口
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

  /** 当前破盾冷却剩余时间（秒）；0 = 不在冷却。 */
  float cooldown();

  /** 破盾冷却总时长（秒）。 */
  float cooldownMax();
}
