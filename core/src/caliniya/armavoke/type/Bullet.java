package caliniya.armavoke.type;

import arc.math.geom.Rect;
import arc.util.pooling.Pool.Poolable;
import arc.util.pooling.Pools;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.type.type.BulletType;
import arc.math.geom.QuadTree.QuadTreeObject;

public class Bullet implements Poolable, QuadTreeObject {
  public BulletType type;
  public Entity owner;
  public TeamTypes team; // 所属团队

  public float x, y;
  public float velX, velY;
  public float rotation;
  public float time = 0f;

  public int id;

  protected Bullet() {}

  /** 工厂方法 */
  public static Bullet create(
      BulletType type,
      Entity owner,
      float x,
      float y,
      float angle,
      float velocityX,
      float velocityY) {
    Bullet b = Pools.obtain(Bullet.class, Bullet::new);
    b.init(type, owner, x, y, angle, velocityX, velocityY);
    return b;
  }

  public void init(
      BulletType type,
      Entity owner,
      float x,
      float y,
      float angle,
      float velocityX,
      float velocityY) {
    this.type = type;
    this.owner = owner;
    this.team = (owner != null) ? owner.team : TeamTypes.Abort;

    this.x = x;
    this.y = y;
    this.rotation = angle;
    this.time = 0f;

    // 计算速度：子弹自身速度 + 发射者惯性
    float bulletSpeed = type.speed;
    float baseVx = (float) Math.cos(Math.toRadians(angle)) * bulletSpeed;
    float baseVy = (float) Math.sin(Math.toRadians(angle)) * bulletSpeed;

    this.velX = baseVx + velocityX;
    this.velY = baseVy + velocityY;

    // 自动添加到处理系统
    Systems.BP.addBullet(this);
  }

  @Override
  public void hitbox(Rect out) {
    out.set(x - type.size / 2f, y - type.size / 2f, type.size, type.size);
  }

  @Override
  public void reset() {
    // 邪修就是保留自身的类型信息，反正创建的时候总是会覆盖的
    // type = null;
    owner = null;
    team = null;
    x = 0;
    y = 0;
    velX = 0;
    velY = 0;
    time = 0;
  }

  public void remove() {
    Pools.free(this);
  }
}
