package caliniya.armavoke.type.ability;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.math.geom.Rect;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.core.meta.stat.Stat;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.core.meta.stat.StatType;
import caliniya.armavoke.core.meta.stat.StatUnit;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.core.meta.ui.Pal;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.ability.api.*;

/**
 * 力场护盾 在一片空间中拦截子弹
 *
 * <p>按照标准的护盾机制执行
 */
public class ShieldFieldAbility extends Ability implements Shield, ForceField {

  /** 最大力场容量。 */
  public float max;

  /** 当前力场容量。 */
  public float current;

  /** 回充速率（以秒为单位设计）。 */
  public float regen;

  /** 回充速率（每帧 = regen / 60）。 */
  public float regenFrame;

  /** 开启时每秒消耗的能量（以秒为单位设计）。 */
  public float energyCost;

  /** 开启时每帧消耗的能量（= cost / 60）。 */
  public float costFrame;

  /** 最大护盾强度（满盾时的强度，默认 2）。 */
  public float maxStrength = 2f;

  // 半径旋转边数(边数为零就是圆形)
  public float radius = 195f, rotation = 0f;

  public int sides = 6;

  public Entity e;

  /** 护盾对各类伤害的百分比抗性（0~1），与单体护盾一致。 */
  public float[] resist = new float[DamageType.values().length];

  public float resist(DamageType type) {
    return resist[type.ordinal()];
  }

  /** 开关。 */
  public boolean active = true;

  @Override
  public ShieldFieldAbility onCreate(Entity e) {
    register();
    this.e = e;
    return (ShieldFieldAbility) super.onCreate(e);
  }

  @Override
  public Entity owner() {
    return e;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    this.active = enabled;
  }

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
    costFrame = energyCost / 60f;
  }

  @Override
  public boolean isActive() {
    return active && current > 0f;
  }

  @Override
  public float energyUse() {
    return active ? costFrame : 0;
  }

  @Override
  public void update(Entity e, float dt) {
    if (!active) return;
    syncFrames();
    if (costFrame > 0 && e.energy <= 0) {
      active = false; // 能量耗尽自动关闭
      return;
    }
    current = Math.min(max, current + regenFrame * dt);
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
  public boolean onBullet(Bullet b) {
    if (e.team == null || e == null || b.team == null) {
      return false;
    }
    if (b.owner.team == e.team) {
      return false;
    }
    float remaining =
        applyDamage(e, b.type.damage, b.type.damageType, b.type.breakShield, b.type.bypassShield);
    return remaining <= 0f;
  }

  public float capacity() {
    return active ? current : 0f;
  }

  @Override
  public void hitbox(Rect out) {
    out.set(e.x - radius, e.y - radius, radius * 2f, radius * 2f);
  }

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

  /** 当前护盾比例（0 ~ 1）。 */
  @Override
  public float percent() {
    return max <= 0f ? 0f : current / max;
  }

  @Override
  public void stats(StatStack stack) {
    stack.add(Stat.shieldMax, max, StatUnit.none, localizedName);
    stack.add(Stat.shieldStrength, maxStrength, StatUnit.percent, localizedName);
    stack.add(Stat.shieldRegen, regen, StatUnit.perSecond, localizedName);
    stack.add(Stat.shieldCost, energyCost, StatUnit.perSecond, localizedName);
    stack.add(Stat.radius, radius, StatUnit.none, localizedName);
    stack.addResists(StatType.function, "stat.shieldResist", resist, localizedName);
  }

  @Override
  public void statAbility(StatStack stat) {
    stat.addRaw(StatType.none, localizedName, null);
    stat.add(Stat.shield, current);
  }

  @Override
  public void draw(Entity e) {
    if (!active || current <= 0f) return;
    Draw.color(Pal.light, 0.6f);
    if (sides <= 0) {
      Lines.circle(e.x, e.y, radius);
    } else {
      Lines.poly(e.x, e.y, sides, radius, rotation);
    }
    Draw.color();
  }

  /** 点是否在力场内（正多边形或圆形）。 */
  @Override
  public boolean contains(float x, float y) {
    if (sides <= 0) {
      return Mathf.dst2(e.x, e.y, x, y) <= radius * radius;
    }
    return Intersector.isInRegularPolygon(sides, e.x, e.y, radius, rotation, x, y);
  }

  @Override
  public ShieldFieldAbility copy() {
    ShieldFieldAbility a = (ShieldFieldAbility) super.copy();
    // 数组需要手动深拷贝
    a.resist = resist.clone();
    return a;
  }

  @Override
  public void write(Writes w) {
    super.write(w);
    w.f(current);
    w.bool(active);
  }

  @Override
  public void read(Reads r) {
    super.read(r);
    current = r.f();
    active = r.bool();
  }
}
