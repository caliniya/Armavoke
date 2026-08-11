package caliniya.armavoke.type.ability;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;

/**
 * 能力基类（类似 Mindustry 的 ability）。
 *
 * <p>特殊机制（护盾、过热等）都做成能力，可组合地附加到单位或建筑上，默认不带。
 */
public abstract class Ability {

  /** 每帧逻辑：回充、耗能、积累热量等。 */
  public void update(Entity e, float dt) {}

  /**
   * 受伤拦截：返回穿透到下一层的伤害。
   *
   * <p>例如护盾能力在这里吸收伤害并返回 0；不拦截时原样返回 damage。
   *
   * @param breakShield 本次攻击是否破盾（无视护盾强度减伤）
   * @param bypassShield 本次攻击是否穿盾（直接穿过护盾）
   */
  public float applyDamage(Entity e, float damage, DamageType type, boolean breakShield, boolean bypassShield) {
    return damage;
  }

  /** 可选视觉绘制（护盾光圈等）。 */
  public void draw(Entity e) {}
}
