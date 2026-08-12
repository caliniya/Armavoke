package caliniya.armavoke.type.enhance;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.type.ability.ShieldAbility;
import caliniya.armavoke.type.ability.ShieldFieldAbility;
import caliniya.armavoke.type.ability.api.Shield;
import caliniya.armavoke.type.enhance.api.*;

/**
 * 护盾强化模组（示例）：绑定实体已有的护盾能力（单体盾/力场盾），
 * <b>开启时提高最大强度与动能抗性，关闭时恢复原值</b>。
 */
public class ShieldBoostEnhancement<T extends Ability & Shield> extends Enhancement implements AbilityBind<T> {

  /** 开启时增加的最大护盾强度。 */
  public float maxStrengthBonus = 1f;

  /** 开启时增加的动能抗性（0~1）。 */
  public float kineticResistBonus = 0.3f;

  private boolean bound;
  private float savedStrength;
  private float savedKineticResist;

  @Override
  public void bindAbility(T s) {
    ability = (Ability)s;
  }

  @Override
  public void onEnable() {
    if (!bound) return;
    if (ability instanceof ShieldAbility s) {
      savedStrength = s.maxStrength;
      savedKineticResist = s.resist(DamageType.Kinetic);
      s.maxStrength += maxStrengthBonus;
      s.resist[DamageType.Kinetic.ordinal()] += kineticResistBonus;
    } else if (ability instanceof ShieldFieldAbility s) {
      savedStrength = s.maxStrength;
      savedKineticResist = s.resist(DamageType.Kinetic);
      s.maxStrength += maxStrengthBonus;
      s.resist[DamageType.Kinetic.ordinal()] += kineticResistBonus;
    }
  }

  @Override
  public void onDisable() {
    if (!bound) return;
    if (ability instanceof ShieldAbility s) {
      s.maxStrength = savedStrength;
      s.resist[DamageType.Kinetic.ordinal()] = savedKineticResist;
    } else if (ability instanceof ShieldFieldAbility s) {
      s.maxStrength = savedStrength;
      s.resist[DamageType.Kinetic.ordinal()] = savedKineticResist;
    }
  }
}
