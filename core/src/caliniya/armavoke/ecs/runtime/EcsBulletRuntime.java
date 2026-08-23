package caliniya.armavoke.ecs.runtime;

import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.type.ability.api.ForceField;
import caliniya.armavoke.type.type.BulletType;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/** Bullet registry and simulation with no legacy bullet objects. */
public final class EcsBulletRuntime {
  private static final IdentityHashMap<BulletType, Integer> ids = new IdentityHashMap<>();
  private static final ArrayList<BulletType> types = new ArrayList<>();

  private EcsBulletRuntime() {}

  public static synchronized int id(BulletType type) {
    if (type == null) return -1;
    Integer current = ids.get(type);
    if (current != null) return current;
    int id = types.size();
    types.add(type);
    ids.put(type, id);
    return id;
  }

  public static synchronized BulletType type(int id) {
    return id >= 0 && id < types.size() ? types.get(id) : null;
  }

  public static Bullet create(BulletType type, Entity owner, float x, float y, float rotation) {
    Bullet bullet = (Bullet) EcsRuntime.requireWorld().create("bullet");
    bullet.bulletBulletTypeId(id(type));
    bullet.bulletOwnerId(owner == null ? -1 : owner.id());
    bullet.bulletDamage(type.damage);
    bullet.bulletSpeed(type.speed);
    bullet.bulletDirectionX(Mathf.cosDeg(rotation));
    bullet.bulletDirectionY(Mathf.sinDeg(rotation));
    bullet.bulletLifetime(type.lifetime);
    bullet.bulletTime(0f);
    bullet.positionX(x);
    bullet.positionXBack(x);
    bullet.positionY(y);
    bullet.positionYBack(y);
    bullet.positionRotation(rotation);
    bullet.positionRotationBack(rotation);
    bullet.teamTeamId(owner == null || owner.team() == null ? -1 : owner.team().ordinal());
    bullet.collisionWidth(type.size);
    bullet.collisionHeight(type.size);
    bullet.collisionSolid(false);
    return bullet;
  }

  public static Bullet create(Entity owner, BulletType type, float x, float y, float rotation) {
    return create(type, owner, x, y, rotation);
  }

  public static void update(EcsWorld world, float delta) {
    EcsEntity[] snapshot = world.snapshot();
    for (EcsEntity value : snapshot) {
      if (!(value instanceof Bullet bullet) || !bullet.active()) continue;
      BulletType type = bullet.type();
      if (type == null) { bullet.remove(); continue; }
      bullet.bulletTime(bullet.bulletTime() + delta);
      bullet.positionX(bullet.x() + bullet.bulletDirectionX() * bullet.bulletSpeed() * delta);
      bullet.positionY(bullet.y() + bullet.bulletDirectionY() * bullet.bulletSpeed() * delta);
      bullet.positionXBack(bullet.positionX());
      bullet.positionYBack(bullet.positionY());
      type.update(bullet);
      if (bullet.bulletTime() >= bullet.bulletLifetime()) { bullet.remove(); continue; }
      boolean intercepted = false;
      for (EcsEntity fieldValue : snapshot) {
        if (!(fieldValue instanceof Entity fieldOwner) || !fieldOwner.active()) continue;
        for (Ability ability : fieldOwner.abilities()) {
          if (ability instanceof ForceField field && field.isActive()
              && field.contains(bullet.x(), bullet.y()) && field.onBullet(bullet)) {
            bullet.remove();
            intercepted = true;
            break;
          }
        }
        if (intercepted) break;
      }
      if (intercepted) continue;
      float radius = Math.max(1f, type.size * 0.5f);
      for (EcsEntity targetValue : snapshot) {
        if (!(targetValue instanceof Entity target) || !target.active() || target.health() <= 0f
            || target.team() == null || target.team() == bullet.team()) continue;
        float hitRadius = radius + Math.max(target.width(), target.height()) * 0.5f;
        if (Mathf.dst2(bullet.x(), bullet.y(), target.x(), target.y()) > hitRadius * hitRadius) continue;
        target.hit(bullet);
        type.hit(bullet, target);
        bullet.remove();
        break;
      }
    }
  }

  public static void clearAll() {
    EcsWorld world = EcsRuntime.world();
    if (world == null) return;
    for (EcsEntity value : world.snapshot()) if (value instanceof Bullet) world.remove(value);
  }
}
