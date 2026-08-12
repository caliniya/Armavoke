package caliniya.armavoke.type.ability;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.core.meta.stat.Stat;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.core.meta.stat.StatUnit;
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

  /** 回充速率（以秒为单位设计）。 */
  public float regen;

  /** 回充速率（每帧 = regen / 60）。 */
  public float regenFrame;

  /** 开启时每秒消耗的能量（以秒为单位设计）。 */
  public float cost;

  /** 开启时每帧消耗的能量（= cost / 60）。 */
  public float costFrame;

  /** 最大护盾强度（满盾时的强度，默认 2）。 */
  public float maxStrength = 2f;

  /** 护盾对各类伤害的百分比抗性（0~1），与单体护盾一致。 */
  public float[] resist = new float[DamageType.values().length];

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

  /**
   * 是否拦截力场内部发射的子弹。
   *
   * <p>false（默认）：放行内部发射的子弹，支持"逼近敌人穿盾输出"； true：拦截范围内所有子弹，适合庇护/压制型力场（贴脸压制敌人火力、保护友军）。
   */
  public boolean interceptInternal = false;

  public ShieldFieldAbility(float max, float radius) {
    super("shieldfield");
    this.max = max;
    this.current = max;
    this.radius = radius;
    this.toggleable = true;
    syncFrames();
  }

  /** 把"每秒"数值同步到"每帧"（update 时再同步一次以支持外部改字段）。 */
  private void syncFrames() {
    regenFrame = regen / 60f;
    costFrame = cost / 60f;
  }

  @Override
  public boolean isActive() {
    return active && current > 0f;
  }

  @Override
  public float energyUse() {
    return active ? costFrame : 0;
  }

  protected void updateField(Entity e, float dt) {
    if (!active) return;
    syncFrames();
    // 能量扣减由 Entity.updateBase 统一按净回复处理
    if (costFrame > 0 && e.energy <= 0) {
      active = false; // 能量耗尽自动关闭
      return;
    }
    current = Math.min(max, current + regenFrame * dt);
    // 力场保持静止（如需旋转可手动设置 rotation）
  }

  /** 减伤机制（与单体护盾一致）：按护盾强度百分比减伤， 支持破盾（无视强度减伤）与穿盾（直接穿过）。 */
  @Override
  public float applyDamage(
      Entity e, float damage, DamageType type, boolean breakShield, boolean bypassShield) {
    // 穿盾：护盾完全不拦截，伤害直接穿过
    if (bypassShield) return damage;
    if (!active || current <= 0) return damage;

    float p = current / max;
    float strength = p * maxStrength;
    // 破盾：无视护盾强度减伤（全伤害扣盾）
    float reduction = breakShield ? 1f : (1f / strength);
    float actual = damage * type.shieldMult * (1f - resist(type)) * reduction;
    current -= actual;
    if (current <= 0) current = 0;

    return 0; // 破盾溢出不传递
  }

  @Override
  public boolean onBullet(Entity e, Bullet b) {
    // 默认放行力场内部发射的子弹；interceptInternal 时拦截范围内所有子弹
    if (!interceptInternal && b.owner != null && contains(e, b.owner.x, b.owner.y)) return false;
    // 与单体护盾完全相同的结算：返回 0 = 完全拦截（子弹消失），>0 = 穿透（放行）
    float remaining =
        applyDamage(e, b.type.damage, b.type.damageType, b.type.breakShield, b.type.bypassShield);
    return remaining <= 0f;
  }

  @Override
  public float capacity() {
    return active ? current : 0f;
  }

  @Override
  public float capacityMax() {
    return max;
  }

  @Override
  public void stats(StatStack stack) {
    stack.add(Stat.shield, max, StatUnit.none, "ShieldFieldAbility");
    stack.add(Stat.shieldStrength, maxStrength, StatUnit.percent, "ShieldFieldAbility");
    stack.add(Stat.shieldRegen, regen, StatUnit.perSecond, "ShieldFieldAbility");
    stack.add(Stat.shieldCost, cost, StatUnit.perSecond, "ShieldFieldAbility");
    stack.add(Stat.radius, radius, StatUnit.none, "ShieldFieldAbility");
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

  @Override
  public ShieldFieldAbility copy() {
    // TODO: Implement this method
    ShieldFieldAbility a = (ShieldFieldAbility) super.copy();
    a.resist = resist.clone();
    return a;
  }
}
