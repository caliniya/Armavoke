package caliniya.armavoke.game;

import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import caliniya.armavoke.game.type.WeaponType;

public class Weapon {
  public final WeaponType type;
  public final Unit owner;

  public float rotation;
  public float reloadTimer = 0f;

  // 当前正在瞄准的世界坐标 (由 Unit 传入)
  public float aimX, aimY;
  // 是否正在开火 (由 Unit 传入)
  public boolean isShooting = false;

  public Weapon(WeaponType type, Unit owner) {
    this.type = type;
    this.owner = owner;
    this.rotation = 0f;

    // 【关键】初始化交替射击的错开时间
    // 如果我是镜像武器，且开启了交替射击，初始自带一半冷却
    // 这样刚出生时按住开火，主武器先射，副武器会等一半时间再射
    if (type.isMirror && type.alternate) {
      this.reloadTimer = type.reload / 2f;
    }
  }

  /**
   * 每帧更新
   *
   * @param targetX 目标X (如果没有目标，可以是鼠标位置或者前置点)
   * @param targetY 目标Y
   * @param shootCmd 是否尝试射击
   */
  public void update(float targetX, float targetY, boolean shootCmd) {
    this.aimX = targetX;
    this.aimY = targetY;
    this.isShooting = shootCmd;

    // 1. 冷却逻辑
    // 只有当 reloadTimer > 0 时才减少，防止减成负数溢出
    if (reloadTimer > 0) {
      reloadTimer -= Time.delta;
    }

    // 2. 旋转瞄准逻辑
    float wx = owner.x + Angles.trnsx(owner.rotation, type.x, type.y);
    float wy = owner.y + Angles.trnsy(owner.rotation, type.x, type.y);

    float targetAngle = Angles.angle(wx, wy, aimX, aimY);
    float mountAngle = targetAngle - owner.rotation - 90;

    this.rotation = Angles.moveToward(this.rotation, mountAngle, type.rotateSpeed * Time.delta);

    // 3. 射击逻辑
    // 只有主武器负责触发射击检测？
    // 或者每个武器独立检测？根据你的需求 "镜像武器不能自行冷却"，
    // 其实是指：镜像武器的开火节奏受主武器影响。

    // 如果我是镜像武器，且是交替模式，我不需要做特殊的"不能自行冷却"限制。
    // 因为初始的 reloadTimer 已经让我错开了。
    // 只要我不去重置另一半的冷却，我们就会各自按周期运行，保持相位差。

    if (isShooting && reloadTimer <= 0) {
      // 检查角度是否对准 (例如偏差小于 10 度)
      if (Angles.within(this.rotation, mountAngle, 10f)) {
        shoot(wx, wy, targetAngle);
      }
    }
  }

  private void shoot(float x, float y, float angle) {
    // 1. 重置自身冷却
    this.reloadTimer = type.reload;

    // 2. 发射子弹 (这里只是伪代码)
    // Bullet.create(type.bullet, x, y, angle ...);

    // 3. 强制同步逻辑 (这是为了防止长期运行后的误差累积)
    // 如果我是主武器，且开启交替模式
    if (!type.isMirror && type.otherSide != -1 && type.alternate) {
      Weapon mirror = owner.weapons.get(type.otherSide);

      // 强制把镜像武器的冷却设为总冷却的一半
      // 这样即使之前因为卡顿或其他原因导致节奏乱了，主武器一开火，立刻纠正副武器节奏
      mirror.reloadTimer = type.reload / 2f;
    }
  }
}
