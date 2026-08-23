package caliniya.armavoke.type;

import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.ecs.runtime.EcsBulletRuntime;
import caliniya.armavoke.type.type.WeaponType;

/** Per-mount weapon state stored inside an ECS RuntimeData component. */
public class Weapon {
  public final WeaponType type;
  public final Unit owner;
  public Entity target;
  public float rotation;
  public float reloadTimer;
  public boolean rotate;
  public float wx, wy;
  public float targetAngle;
  public float mountAngle;

  public Weapon(WeaponType type, Unit owner) {
    this.type = type;
    this.owner = owner;
    this.rotate = type != null && type.rotate;
    this.rotation = owner == null ? 0f : owner.rotation();
  }

  public void update(float delta, boolean canShoot) {
    if (type == null || owner == null || !owner.active()) return;
    float base = owner.rotation() - 90f;
    wx = owner.x() + Angles.trnsx(base, type.x, type.y);
    wy = owner.y() + Angles.trnsy(base, type.x, type.y);
    reloadTimer += delta;
    if (target != null && target.active()) {
      targetAngle = Angles.angle(wx, wy, target.x(), target.y());
      rotation = rotate ? Angles.moveToward(rotation, targetAngle, type.rotateSpeed * delta) : owner.rotation();
    }
    if (!canShoot || target == null || type.bullet == null || owner.locked()
        || reloadTimer < type.reload || Mathf.dst2(wx, wy, target.x(), target.y()) > type.range * type.range) return;
    float sx = wx + Angles.trnsx(rotation, type.shootX, type.shootY);
    float sy = wy + Angles.trnsy(rotation, type.shootX, type.shootY);
    EcsBulletRuntime.create(type.bullet, owner, sx, sy, rotation);
    owner.heat(owner.heat() + type.heatPerShot);
    reloadTimer = 0f;
  }

  public void draw() {
    if (type == null || type.region == null) return;
    Draw.rect(type.region, wx, wy, type.region.width, type.region.height, rotation - 90f);
  }
}
