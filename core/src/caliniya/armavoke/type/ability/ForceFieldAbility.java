package caliniya.armavoke.type.ability;

import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.math.geom.Rect;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.type.Bullet;

/**
 * 力场能力基类：生成一个**正多边形或圆形**能量场。
 *
 * <p>所有带力场能力的实体由注册表 {@link #entities} 维护， 子弹处理系统遍历注册表做「AABB 粗筛 + 多边形/圆形精判」， 然后回调 {@link #onBullet}
 * 让子类决定效果。
 *
 * <p>子类实现具体力场：护盾力场（拦截子弹）、压制场、修复场、效果场等。
 */
public abstract class ForceFieldAbility extends Ability {

  /** 正多边形边数（0 = 圆形）。 */
  public int sides = 6;

  /** 力场半径（圆形或多边形外接圆半径）。 */
  public float radius;

  /** 多边形旋转角度。 */
  public float rotation;

  /** 力场实体注册表：当前所有**生效**的力场实体。 由基类 update 维护，BulletProcess（子弹线程）遍历做拦截/效果。 */
  public static final Ar<Entity> entities = new Ar<>(false, 8);
  
  public ForceFieldAbility(String name){
    super(name);
  }

  /** 该力场当前是否生效（子类覆写，如护盾力场看 active 与容量）。 */
  public boolean isActive() {
    return true;
  }
  
  @Override
  public Ability oncteate(Entity e) {
    entities.add(e);
    return this;
  }
  
  
  @Override
  public void update(Entity e, float dt) {
    // 注册表维护：生效时注册实体，失效时注销
    synchronized (entities) {
      if (isActive()) {
        if (!entities.contains(e, true)) entities.add(e);
      } else {
        entities.remove(e, true);
      }
    }
    updateField(e, dt);
  }

  /** 子类每帧逻辑（回充、耗能、旋转等）。 */
  protected abstract void updateField(Entity e, float dt);

  /** 覆盖力场的 AABB（四叉树粗筛候选子弹）。 */
  public void hitbox(Entity e, Rect out) {
    out.set(e.x - radius, e.y - radius, radius * 2f, radius * 2f);
  }

  /** 点是否在力场内（正多边形或圆形）。 */
  public boolean contains(Entity e, float x, float y) {
    if (sides <= 0) {
      return Mathf.dst2(e.x, e.y, x, y) <= radius * radius;
    }
    return Intersector.isInRegularPolygon(sides, e.x, e.y, radius, rotation, x, y);
  }

  /** 子弹进入力场回调。返回 true 表示**拦截**该子弹（由子弹系统移除）。 默认放行，子类覆写实现具体效果。 */
  public boolean onBullet(Entity e, Bullet b) {
    return false;
  }

  /** 当前容量（供血条等显示，默认 0）。 */
  public float capacity() {
    return 0f;
  }

  /** 最大容量。 */
  public float capacityMax() {
    return 0f;
  }
}
