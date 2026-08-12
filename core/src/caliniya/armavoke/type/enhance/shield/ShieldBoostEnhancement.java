package caliniya.armavoke.type.enhance.shield;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.type.Enhancement;
import caliniya.armavoke.type.ability.ShieldAbility;
import caliniya.armavoke.type.ability.ShieldFieldAbility;

/**
 * 护盾强化插件实体（示例）：绑定实体已有的护盾能力（单体盾/力场盾），
 * <b>开启时提高最大强度与动能抗性（配置取自 {@link ShieldBoostEnhancementType}），关闭时恢复原值</b>。
 */
public class ShieldBoostEnhancement extends Enhancement {

  private boolean bound;
  private float savedStrength;
  private float savedKineticResist;

  /** 读档/挂载后恢复绑定：从实体重新查找护盾能力（单体盾优先，否则力场盾）。 */
  @Override
  public void rebind(Entity e) {
    ShieldAbility sa = e.getAbility(ShieldAbility.class);
    ability = sa != null ? sa : e.getAbility(ShieldFieldAbility.class);
    bound = ability != null;
  }

  @Override
  public void onEnable() {
    if (!bound || !(type instanceof ShieldBoostEnhancementType t)) return;
    if (ability instanceof ShieldAbility s) {
      savedStrength = s.maxStrength;
      savedKineticResist = s.resist(DamageType.Kinetic);
      s.maxStrength += t.maxStrengthBonus;
      s.resist[DamageType.Kinetic.ordinal()] += t.kineticResistBonus;
    } else if (ability instanceof ShieldFieldAbility s) {
      savedStrength = s.maxStrength;
      savedKineticResist = s.resist(DamageType.Kinetic);
      s.maxStrength += t.maxStrengthBonus;
      s.resist[DamageType.Kinetic.ordinal()] += t.kineticResistBonus;
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
