package caliniya.armavoke.type.ability;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.type.Bullet;

/**
 * 护盾力场：**空间拦截**进入力场的子弹（正多边形或圆形）。
 *
 * <p>拦截时按子弹伤害扣减力场容量；容量耗尽或关闭后不再拦截（注册表自动注销）。
 */
public class ShieldFieldAbility extends ForceFieldAbility {

  /** 最大力场容量。 */
  public float max;

  /** 当前力场容量。 */
  public float current;

  /** 回充速率（每秒）。 */
  public float regen;

  /** 开启时每秒消耗的能量。 */
  public float cost;

  /** 开关。 */
  public boolean active = true;

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    this.active = enabled;
  }

  /**
   * 是否拦截力场内部发射的子弹。
   *
   * <p>false（默认）：放行内部发射的子弹，支持"逼近敌人穿盾输出"； true：拦截范围内所有子弹，适合庇护/压制型力场（贴脸压制敌人火力、保护友军）。
   */
  public boolean interceptInternal = false;

  public ShieldFieldAbility(float max, float radius) {
    this.max = max;
    this.current = max;
    this.radius = radius;
    this.toggleable = true;
  }

  @Override
  public boolean isActive() {
    return active && current > 0f;
  }

  @Override
  public float energyUse() {
    return active ? cost : 0;
  }

  protected void updateField(Entity e, float dt) {
    if (!active) return;
    // 能量扣减由 Entity.updateBase 统一按净回复处理
    if (cost > 0 && e.energy <= 0) {
      active = false; // 能量耗尽自动关闭
      return;
    }
    current = Math.min(max, current + regen * dt);
    // 力场保持静止（如需旋转可手动设置 rotation）
  }

  @Override
  public boolean onBullet(Entity e, Bullet b) {
    if (!active || current <= 0f) return false;
    // 默认放行力场内部发射的子弹；interceptInternal 时拦截范围内所有子弹
    if (!interceptInternal && b.owner != null && contains(e, b.owner.x, b.owner.y)) return false;
    current -= b.type.damage;
    if (current < 0f) current = 0f;
    return true; // 拦截
  }

  @Override
  public float capacity() {
    return current;
  }

  @Override
  public float capacityMax() {
    return max;
  }

  @Override
  public void draw(Entity e) {
    if (!active || current <= 0f) return;
    Draw.color(Color.sky, 0.4f);
    if (sides <= 0) {
      Lines.circle(e.x, e.y, radius);
    } else {
      Lines.poly(e.x, e.y, sides, radius, rotation);
    }
    Draw.color();
  }
}
