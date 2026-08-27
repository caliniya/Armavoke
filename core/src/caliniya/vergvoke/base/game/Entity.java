package caliniya.vergvoke.base.game;

import arc.math.Mathf;
import arc.math.geom.QuadTree.QuadTreeObject;
import arc.math.geom.Rect;
import arc.util.pooling.Pool.Poolable;
import caliniya.vergvoke.core.meta.stat.Stat;
import caliniya.vergvoke.core.meta.stat.StatStack;
import caliniya.vergvoke.core.meta.stat.StatUnit;
import caliniya.vergvoke.game.data.TeamData;
import caliniya.vergvoke.type.ability.*;
import caliniya.vergvoke.type.ability.api.*;
import caliniya.vergvoke.base.tool.Ar;
import caliniya.vergvoke.type.Bullet;
import caliniya.vergvoke.type.ability.api.Shield;
import caliniya.vergvoke.type.Enhancement;
import caliniya.vergvoke.type.enhance.api.Updatable;
import caliniya.vergvoke.type.module.ItemModule;
import caliniya.vergvoke.base.type.DamageType;
import caliniya.vergvoke.base.type.TeamTypes;
import arc.util.io.*;
import caliniya.vergvoke.type.module.LiquidModule;
import caliniya.vergvoke.type.module.PowerModule;

/** 游戏实体基类。 实现了 {@link QuadTreeObject} 以便放入 EntityGroup 的四叉树空间索引。 */
public abstract class Entity implements Poolable, QuadTreeObject {

  // --- 公共坐标 ---
  public float x, y;

  // --- 公共状态 ---
  public volatile float health;
  public float maxHealth;
  public int id;

  public volatile TeamTypes team;
  public volatile TeamData teamData;

  // --- 公共组件 ---
  public ItemModule item;
  public LiquidModule liquid;
  public PowerModule power;

  // 此实体所锁定的目标
  public Entity target;

  // --- 战斗基础属性（特殊机制如护盾/过热走能力）---
  /** 当前护甲容量（护甲血条，0 = 无护甲）。 */
  public float armor;

  /** 最大护甲容量。 */
  public float armorMax;

  /** 护甲强度：固定减伤值（护甲存在时生效，可直接减到 0）。 */
  public float armorValue;

  /** 能量池：当前能量。 */
  public float energy;

  /** 能量池上限。 */
  public float energyMax;

  /** 能量恢复速率（每秒）。 */
  public float energyRegen;

  // 当前热量与最大热量
  public float heat, heatMax = 0f;

  public float heatSpeed = 0f;

  /** 这个实体是否具有过热能力 */
  public boolean heatable = false;

  /** 实体当前是否处于锁定状态(可能但不限于是由于热量造成的) */
  public boolean locked;

  /** 护甲对各类伤害的百分比抗性（0~1），索引 = DamageType.ordinal()。 */
  public float[] armorResist = new float[DamageType.values().length];

  /** 护甲对指定伤害类型的抗性（0~1）。 */
  public float armorResist(DamageType type) {
    return armorResist[type.ordinal()];
  }

  /** 能力列表：护盾/过热等可组合能力，默认不带。 */
  public Ar<Ability> abilities = new Ar<>();

  /** 附加一个能力。 */
  public void addAbility(Ability ability) {
    if (ability != null) abilities.add(ability.onCreate(this));
  }

  public Entity() {}

  public abstract void update(float dt);

  public abstract void draw();

  public abstract void remove();

  public abstract void kill();

  public abstract void write(Writes w);

  public abstract void read(Reads r);

  public void hit(Bullet b) {
    applyDamage(
        b.type.damage,
        b.type.damageType,
        b.type.breakArmor,
        b.type.bypassArmor,
        b.type.breakShield,
        b.type.bypassShield);

    // 动能击退：沿子弹方向施加冲量（力度由 BulletType.knockbackForce 配置）
    if (b.type.damageType.knockback && b.type.knock > 0f) {
      knock(b.rotation, b.type.knock);
    }
  }

  /** 击退冲量分量（后台子弹线程写、主线程读，volatile 保证可见性）。 */
  public volatile float knockX, knockY;

  /** 施加击退：方向（角度）+ 击退量。命中时调用，内部一次三角计算。很显然建筑不能被击退 */
  public void knock(float dir, float force) {}

  /** 每帧更新战斗基础属性：能量恢复 + 能力更新（护盾回充/耗能等）。 */
  public void updateBase(float dt) {

    float cool = heatSpeed / 60f * dt;
    if (locked) {
      // 锁定期间持续散热，归零后恢复
      heat -= cool;
      if (heat <= 0f) {
        heat = 0f;
        locked = false;
      }
      return;
    } else {
      heat = Math.max(0f, heat - cool);
    }

    // 净回复 = 基础回复 - 所有激活能力的能耗（避免能量条在满值附近抖动）
    float use = 0f;
    for (Ability a : abilities) {
      use += a.energyUse();
    }
    float net = energyRegen / 60f - use; // energyRegen 以秒设计，这里转成每帧
    if (net != 0f) {
      energy = Math.min(energyMax, energy + net * dt);
    }
    for (Ability a : abilities) {
      a.update(this, dt);
    }
    // 强化模组：只需每帧更新的（实现 Updatable 接口的）
    for (Updatable u : updatableEnhancements) {
      u.update(this, dt);
    }
  }

  /** 强化模组列表（全部，用于来源记录/开关管理）。 */
  public Ar<Enhancement> enhancements = new Ar<>();

  /** 只需每帧更新的强化模组（实现 {@link Updatable} 接口的），避免空转。 */
  public Ar<Updatable> updatableEnhancements = new Ar<>();

  /** 挂载一个强化模组（运行时安装/读档恢复）：绑实体 → 恢复绑定 → 入列表 → 需要每帧则入 updatable 列表 → 初始开启则应用。 */
  public void addEnhancement(Enhancement enh) {
    if (enh == null) return;
    enh.entity = this;
    enh.type.rebind(enh);
    enhancements.add(enh);
    if (enh instanceof Updatable u) {
      updatableEnhancements.add(u);
    }
    if (enh.enabled) {
      enh.type.onEnable(enh);
    }
  }

  public <T extends Ability> T getAbility(Class<T> S) {
    for (Ability a : abilities) {
      if (S.isInstance(a)) {
        return S.cast(a);
      }
    }
    return null;
  }

  /** 向实体添加热量（由武器/能力/模组等热源调用）。 实体没有过热能力（或储热上限为 0）时忽略——即没有过热机制。 */
  public void addHeat(float amount) {
    if (heatable) {
      heat += amount;
    }
  }

  /** 所有护盾能力（单体护盾 + 力场等）的当前总容量。 */
  public float totalShield() {
    float total = 0f;
    for (Ability a : abilities) {
      if (a instanceof Shield s) {
        total += s.capacity();
      }
    }
    return total;
  }

  /** 批量开关所有可切换（toggleable）的能力。 */
  public void setAllAbilities(boolean enabled) {
    for (Ability a : abilities) {
      if (a.toggleable) a.setEnabled(enabled);
    }
  }

  /** 所有护盾能力的最大总容量。 */
  public float totalShieldMax() {
    float total = 0f;
    for (Ability a : abilities) {
      if (a instanceof Shield s) {
        total += s.capacityMax();
      }
    }
    return total;
  }

  /**
   * 对实体造成一次伤害（三层结算：能力拦截 → 护甲 → 本体）。
   *
   * <ol>
   *   <li>每个能力依次拦截（护盾吸收等），返回穿透到下一层的伤害；
   *   <li>护甲层：对甲倍率 × (1 - 护甲对该类型抗性)，再减护甲强度（最低 0）；
   *   <li>本体扣血，归零摧毁。
   * </ol>
   */
  public void applyDamage(float damage, DamageType type) {
    applyDamage(damage, type, false, false, false, false);
  }

  /**
   * 对实体造成一次伤害（三层结算：能力拦截 → 护甲 → 本体）。
   *
   * @param breakArmor 破甲：无视护甲的固定减伤值（护甲容量照扣）
   * @param bypassArmor 穿甲：直接穿过护甲层攻击核心
   * @param breakShield 破盾：无视护盾的强度减伤（护盾容量照扣）
   * @param bypassShield 穿盾：直接穿过护盾层
   */
  public void applyDamage(
      float damage,
      DamageType type,
      boolean breakArmor,
      boolean bypassArmor,
      boolean breakShield,
      boolean bypassShield) {
    // 1. 能力拦截（护盾等），全部吸收则直接结束
    for (Ability a : abilities) {
      damage = a.applyDamage(this, damage, type, breakShield, bypassShield);
    }
    if (damage <= 0f) return;

    // 2. 护甲层（容量 > 0 时存在；穿甲直接跳过护甲打核心）
    if (!bypassArmor && armor > 0f) {
      float armorReduce = breakArmor ? 0f : armorValue;
      float actual = Math.max(0f, damage * type.armorMult * (1f - armorResist(type)) - armorReduce);
      if (actual <= 0f) return; // 被护甲完全挡下
      armor -= actual;
      if (armor < 0f) armor = 0f;
      return; // 护甲破：剩余伤害不传递
    }

    // 3. 本体（无护甲或被穿甲跳过：无抗性减伤、无固定减伤）
    damage = damage * type.armorMult;
    health -= damage;
    if (health <= 0f) {
      health = 0f;
      kill();
    }
  }

  public void stat(StatStack stat) {
    // 构建统计结构并注册动态数值源（live）：渲染端每帧经 getData 自动取最新值，无需反复调用本方法
    stat.get(Stat.health, health, maxHealth).live = () -> health;
    stat.get(Stat.armor, armor, armorMax).live = () -> armor;
    stat.get(Stat.shield, totalShield(), totalShieldMax()).live = () -> totalShield();
    stat.get(Stat.energy, energy, energyMax).live = () -> energy;
    if (heatable) stat.get(Stat.heat, heat, heatMax).live = () -> heat;
    if (power != null) stat.get(Stat.power, power.power, power.powerMax).live = () -> power.power;
    for (Ability a : abilities) {
      a.statAbility(stat);
    }
  }

  /** 返回实体的碰撞盒尺寸（直径）。 子类应该覆盖此方法以提供准确的碰撞体大小。 默认返回 8 像素。 */
  public float hitboxSize() {
    return 8f;
  }

  /** 填充实体的粗略包围盒。 该包围盒不能小于实体实际范围，但可以偏大。 */
  @Override
  public void hitbox(Rect out) {
    float half = hitboxSize() / 2f;
    out.set(x - half, y - half, hitboxSize(), hitboxSize());
  }

  @Override
  public void reset() {
    x = 0;
    y = 0;
    health = 0;
    maxHealth = 0;
    armor = 0;
    armorMax = 0;
    armorValue = 0;
    energy = 0;
    energyMax = 0;
    energyRegen = 0;
    java.util.Arrays.fill(armorResist, 0f);
    for (Ability a : abilities) {
      if (a instanceof ForceField f) {
        ForceField.force.remove(f);
      }
    }
    abilities.clear();
    enhancements.clear();
    updatableEnhancements.clear();
    team = null;
    item = null;
    liquid = null;
    power = null;
    target = null;
  }
}
