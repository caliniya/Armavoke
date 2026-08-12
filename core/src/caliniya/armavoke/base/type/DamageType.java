package caliniya.armavoke.base.type;

import arc.Core;

/**
 * 伤害类型。
 *
 * <p>倍率只作用于当前正在结算的那一层：打在护盾上用对盾倍率，打在护甲/本体上用对甲倍率。
 */
public enum DamageType {
  /** 能量：破盾主力。 */
  Energy(1.5f, 1.0f, false),
  /** 热能：破甲主力。 */
  Thermal(1.0f, 1.5f, false),
  /** 动能：均等伤害，附带击退（击退机制以后实现）。 */
  Kinetic(1.0f, 1.0f, true);

  /** 对护盾伤害倍率。 */
  public final float shieldMult;

  /** 对护甲伤害倍率。 */
  public final float armorMult;

  /** 是否造成击退。 */
  public final boolean knockback;

  /** 本地化名称（如 "能量"），用于抗性列表等展示。 */
  public final String localizedName;

  DamageType(float shieldMult, float armorMult, boolean knockback) {
    this.shieldMult = shieldMult;
    this.armorMult = armorMult;
    this.knockback = knockback;
    this.localizedName = Core.bundle.get("damageType." + name());
  }
}
