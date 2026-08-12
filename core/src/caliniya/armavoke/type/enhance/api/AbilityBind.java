package caliniya.armavoke.type.enhance.api;

import caliniya.armavoke.type.ability.Ability;

/** 绑定能力：强化实体已有能力（如护盾抗性/强度）的模组实现此接口。 */
public interface AbilityBind<T extends Ability> {

  /** 挂载时从实体查找目标能力，存入 {@link Enhancement#ability}（找不到则强化无效）。 */
  void bindAbility(T e);
}
