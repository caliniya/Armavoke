package caliniya.armavoke.type;

import arc.math.Angles;
import arc.util.Time;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.type.type.WeaponType;

public class Weapon {
  public final WeaponType type;
  public final Unit owner;

  public float rotation;
  public float reloadTimer = 0f;

  public float aimX, aimY;
  public boolean isShooting = false;

  public Weapon(WeaponType type, Unit owner) {
    this.type = type;
    this.owner = owner;
    this.rotation = 0f;

    if (type.isMirror && type.alternate) {
      this.reloadTimer = type.reload / 2f;
    }
  }

  /**
   * 统一更新方法 (由 Unit.updateWeapons 调用)
   *
   * @param dt 时间增量 (Time.delta)
   * @param targetX 目标X
   * @param targetY 目标Y
   * @param shootCmd 是否开火
   */
  public void update(float dt, float targetX, float targetY, boolean shootCmd) {
    this.aimX = targetX;
    this.aimY = targetY;
    this.isShooting = shootCmd;

    // 1. 冷却逻辑 (依赖 dt)
    if (reloadTimer > 0) {
      reloadTimer -= dt;
    }

    // 2. 旋转逻辑 (依赖 dt)
    // 计算武器的世界坐标 (用于计算角度)
    // 注意：这里用 owner.rotation，因为武器是挂在单位身上的
    float wx = owner.x + Angles.trnsx(owner.rotation, type.x, type.y);
    float wy = owner.y + Angles.trnsy(owner.rotation, type.x, type.y);

    float targetAngle = Angles.angle(wx, wy, aimX, aimY);
    // 目标相对角度 = 目标绝对角度 - 单位朝向 - 90 (素材默认朝上)
    float mountAngle = targetAngle - owner.rotation - 90;

    // 平滑旋转
    this.rotation = Angles.moveToward(this.rotation, mountAngle, type.rotateSpeed * dt);

    // 3. 射击判定
    if (isShooting && reloadTimer <= 0) {
      // 允许一定的射击角度误差 (1度)
      if (Angles.within(this.rotation, mountAngle, 1f)) {
        shoot(wx, wy, targetAngle);
      }
    }
  }

  private void shoot(float wx, float wy, float angle) {
    this.reloadTimer = type.reload;

    // 计算枪口位置
    float bulletX = wx + Angles.trnsx(angle, type.shootX, type.shootY);
    float bulletY = wy + Angles.trnsy(angle, type.shootX, type.shootY);

    if (type.bullet != null) {
      // 传递 owner 的速度用于惯性叠加
      Bullet.create(type.bullet, owner, bulletX, bulletY, angle, owner.speedX, owner.speedY);
    }

    // 交替射击同步
    if (!type.isMirror && type.otherSide != -1 && type.alternate) {
      if (type.otherSide < owner.weapons.size) {
        Weapon mirror = owner.weapons.get(type.otherSide);
        if (mirror != null) {
          mirror.reloadTimer = type.reload / 2f;
        }
      }
    }
  }
}
