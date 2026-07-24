package caliniya.armavoke.world.defence.turret;

import arc.math.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.*;
import arc.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.io.*;
import caliniya.armavoke.base.game.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.world.*;

public class Turret extends Block {

  public float range = 100f;
  public float rotateSpeed = 500f;
  public float reloadTime = 10f;
  public BulletType bulletType;

  public TextureRegion baseRegion;

  public Turret(String name) {
    super(name);
    this.capacity = 0;
  }

  @Override
  public void load() {
    super.load();
    baseRegion = Core.atlas.find(name + "-base");
    if (bulletType != null) bulletType.load();
  }

  @Override
  public void update(Building b, float dt) {
    // 目标由 EntityProces 后台线程维护，这里只做射程 + 血量校验
    if (b.target != null) {
      float dst2 = Mathf.dst2(b.x, b.y, b.target.x, b.target.y);
      if (b.target.health <= 0 || dst2 > range * range) {
        b.target = null;
      }
    }

    // 瞄准与射击
    if (b.target != null) {
      float targetAngle = Angles.angle(b.x, b.y, b.target.x, b.target.y);
      b.rotation = Angles.moveToward(b.rotation, targetAngle, rotateSpeed * dt);

      b.reload += dt;

      if (b.reload >= reloadTime && Angles.angleDist(b.rotation, targetAngle) < 5f) {
        shoot(b, b.rotation);
        b.reload = 0;
      }
    }
  }

  @Override
  public void draw(Building b) {
    Draw.rect(baseRegion, b.x, b.y, b.angle * 90f);

    if (region != null) {
      Draw.rect(region, b.x, b.y, b.rotation - 90f);
    }
  }

  private void shoot(Building b, float angle) {
    float x = b.x;
    float y = b.y;
    Bullet.create(bulletType, b, x, y, angle, 0, 0);
  }

  /** 覆写 Block 的空 findTarget，由 EntityProces 线程调用，线程安全。 */
  @Override
  public Entity findTarget(Building b) {
    return Entities.closestEnemy(b.team, b.x, b.y, range);
  }

  @Override
  public void write(Building b, Writes w) {
    w.f(b.rotation);
    w.f(b.reload);
  }

  @Override
  public void read(Building b, Reads r) {
    b.rotation = r.f();
    b.reload = r.f();
  }
}
