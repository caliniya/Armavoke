package caliniya.armavoke.system.world;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.tool.EntityAr;
import caliniya.armavoke.ecs.runtime.EcsBulletRuntime;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.type.Bullet;

/** Compatibility facade. Bullet ownership and updates now belong to the ECS bullet system. */
@Deprecated
public class BulletProcess extends caliniya.armavoke.system.System<BulletProcess> {
  public final Object BULLET_LOCK = EcsBulletRuntime.lock();
  public final EntityAr<Bullet> activeBullets = EcsBulletRuntime.activeBullets();

  @Override
  public BulletProcess init() {
    return super.init(false);
  }

  public void resizeTree(float width, float height) {
    EcsBulletRuntime.resize(width, height);
  }

  public void addBullet(Bullet bullet) {
    EcsBulletRuntime.add(bullet);
  }

  public void addBullets(Bullet... bullets) {
    EcsBulletRuntime.add(bullets);
  }

  public void clearAll() {
    EcsBulletRuntime.clearAll();
  }

  @Override
  public void update(float delta) {
    if (Systems.ECS != null && Systems.ECS.world() != null) {
      EcsBulletRuntime.update(Systems.ECS.world(), delta);
    }
  }

  public void drainFreshKills(Ar<Entity> destination) {
    EcsBulletRuntime.drainFreshKills(destination);
  }

  public void debug() {
    EcsBulletRuntime.debug();
  }
}
