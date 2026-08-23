package caliniya.armavoke.ecs.runtime;

import arc.math.geom.Rect;
import arc.util.Log;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.tool.EntityAr;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.ability.api.ForceField;
import java.util.IdentityHashMap;

/** ECS-owned bullet storage, collision processing, pooling and render-buffer publication. */
public final class EcsBulletRuntime {
  private static final Object bulletLock = new Object();
  private static final Object killLock = new Object();
  private static final Ar<Integer> freeIds = new Ar<>(false, 256);
  private static final EntityAr<Bullet> pending = new EntityAr<>(bullet -> bullet.id);
  private static final EntityAr<Bullet> active = new EntityAr<>(bullet -> bullet.id);
  private static EntityAr<Bullet> renderBuffer = new EntityAr<>(bullet -> bullet.id);
  private static final Ar<Bullet> removals = new Ar<>(false, 256);
  private static final Ar<Entity> freshKills = new Ar<>(false, 64);
  private static final Rect aabb = new Rect();
  private static int nextId = 1;

  private EcsBulletRuntime() {}

  public static Object lock() {
    return bulletLock;
  }

  public static EntityAr<Bullet> activeBullets() {
    return active;
  }

  private static synchronized int allocateId() {
    return freeIds.isEmpty() ? nextId++ : freeIds.remove(freeIds.size - 1);
  }

  private static synchronized void recycleId(int id) {
    if (id > 0 && !freeIds.contains(id)) freeIds.add(id);
  }

  public static void add(Bullet bullet) {
    if (bullet == null) return;
    if (bullet.id <= 0) bullet.id = allocateId();
    GameEcsBridge.register(bullet);
    synchronized (pending) {
      pending.add(bullet);
    }
  }

  public static void add(Bullet... bullets) {
    if (bullets == null) return;
    for (Bullet bullet : bullets) add(bullet);
  }

  public static void resize(float width, float height) {
    active.resize(0f, 0f, width, height);
  }

  public static void update(EcsWorld world, float delta) {
    synchronized (pending) {
      pending.each(
          bullet -> {
            if (bullet != null) active.add(bullet);
          });
      pending.clear();
    }
    removals.clear();
    active.eachWrited(
        bullet -> {
          bullet.time += delta;
          if (bullet.type == null || bullet.time >= bullet.type.lifetime) {
            markForRemoval(bullet);
            return;
          }
          bullet.x += bullet.velX * delta;
          bullet.y += bullet.velY * delta;
          active.move(bullet, bullet.x, bullet.y);
          GameEcsBridge.syncFromLegacy(bullet);
        });

    interceptForceFields();
    active.each(
        bullet -> {
          if (removals.contains(bullet) || bullet.type == null) return;
          Entities.closestEnemy(
              bullet.team,
              bullet.x,
              bullet.y,
              bullet.type.size,
              target -> {
                float previousHealth = target.health;
                bullet.type.hit(bullet, target);
                GameEcsBridge.syncFromLegacy(target);
                markForRemoval(bullet);
                if (previousHealth > 0f && target.health <= 0f) {
                  synchronized (killLock) {
                    freshKills.add(target);
                  }
                }
              });
        });

    removals.each(
        bullet -> {
          active.remove(bullet);
          GameEcsBridge.unregister(bullet);
          recycleId(bullet.id);
        });

    renderBuffer.clear();
    active.each(renderBuffer::add);
    synchronized (bulletLock) {
      if (WorldData.bullets != null) {
        EntityAr<Bullet> previous = WorldData.bullets;
        WorldData.bullets = renderBuffer;
        renderBuffer = previous;
      }
    }
    removals.each(Bullet::remove);
    removals.clear();
  }

  private static void markForRemoval(Bullet bullet) {
    if (!removals.contains(bullet)) removals.add(bullet);
  }

  private static void interceptForceFields() {
    synchronized (ForceField.force) {
      Ar<ForceField> cleanup = null;
      for (int i = 0; i < ForceField.force.size; i++) {
        ForceField field = ForceField.force.get(i);
        if (field == null) {
          if (cleanup == null) cleanup = new Ar<>(false, 4);
          cleanup.add(field);
          continue;
        }
        field.hitbox(aabb);
        active.intersect(
            aabb.x,
            aabb.y,
            aabb.width,
            aabb.height,
            bullet -> {
              if (!removals.contains(bullet)
                  && field.contains(bullet.x, bullet.y)
                  && field.onBullet(bullet)) markForRemoval(bullet);
            });
      }
      if (cleanup != null) for (ForceField field : cleanup) ForceField.force.remove(field, true);
    }
  }

  public static void drainFreshKills(Ar<Entity> destination) {
    synchronized (killLock) {
      destination.add(freshKills);
      freshKills.clear();
    }
  }

  public static void clearAll() {
    IdentityHashMap<Bullet, Boolean> all = new IdentityHashMap<>();
    synchronized (pending) {
      pending.each(bullet -> all.put(bullet, Boolean.TRUE));
      pending.clear();
    }
    active.each(bullet -> all.put(bullet, Boolean.TRUE));
    renderBuffer.each(bullet -> all.put(bullet, Boolean.TRUE));
    if (WorldData.bullets != null) {
      synchronized (bulletLock) {
        WorldData.bullets.each(bullet -> all.put(bullet, Boolean.TRUE));
        WorldData.bullets.clear();
      }
    }
    active.clear();
    renderBuffer.clear();
    for (Bullet bullet : all.keySet()) {
      GameEcsBridge.unregister(bullet);
      recycleId(bullet.id);
      bullet.remove();
    }
    removals.clear();
    synchronized (killLock) {
      freshKills.clear();
    }
  }

  public static void debug() {
    Log.info(
        "Bullets(ECS): active="
            + active.size()
            + ", pending="
            + pending.size()
            + ", render="
            + renderBuffer.size()
            + ", nextId="
            + nextId
            + ", freeIds="
            + freeIds.size);
  }
}
