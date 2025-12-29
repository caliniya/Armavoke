package caliniya.armavoke.system.world;

import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.RouteData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;

public class UnitMath extends BasicSystem<UnitMath> {

  private Ar<Unit> processList = new Ar<>();

  @Override
  public UnitMath init() {
    return super.init(true);
  }

  @Override
  public void update() {
    processList.clear();
    synchronized (WorldData.moveunits) {
      processList.addAll(WorldData.moveunits);
    }

    // 获取当前帧的时间增量 (delta)
    // 假设 BasicSystem 已经正确计算了 this.delta
    float dt = this.delta;

    for (int i = 0; i < processList.size; ++i) {
      Unit u = processList.get(i);

      if (u == null || u.health <= 0) {
        synchronized (WorldData.moveunits) {
          WorldData.moveunits.remove(u);
        }
        continue;
      }

      if (!u.pathed) {
        calculatePath(u);
        u.pathed = true;
        u.velocityDirty = true;
      }

      calculateVelocity(u, dt);
    }
  }

  // calculatePath 保持不变...
  private void calculatePath(Unit u) {
    int sx = (int) (u.x / WorldData.TILE_SIZE);
    int sy = (int) (u.y / WorldData.TILE_SIZE);
    int tx = (int) (u.targetX / WorldData.TILE_SIZE);
    int ty = (int) (u.targetY / WorldData.TILE_SIZE);

    if (sx != tx || sy != ty) {
      // 这里可以根据 Unit 的 type.size 和 capability 传参
      // 暂时硬编码 2, 1
      u.path = RouteData.findPath(sx, sy, tx, ty, 2, 1);

      if (u.path != null && !u.path.isEmpty()) {
        u.path.remove(0);
      }
      u.pathIndex = 0;

      if (u.path == null) {
        stopAndRemove(u);
      } else if (u.path.isEmpty()) {
        // 路径本来就短，或者移除后为空，交给 velocity 处理
        ;
      }
    } else {
      if (u.path != null) u.path.clear();
      u.velocityDirty = true;
    }
  }

  /**
   * 计算速度与转向
   *
   * @param dt 时间增量
   */
  private void calculateVelocity(Unit u, float dt) {
    if (u.path == null || u.path.isEmpty()) {
      handleFinalApproach(u, dt);
      return;
    }

    if (u.pathIndex < u.path.size) {
      float nextX, nextY;

      if (u.pathIndex == u.path.size - 1) {
        nextX = u.targetX;
        nextY = u.targetY;
      } else {
        Point2 node = u.path.get(u.pathIndex);
        nextX = node.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
        nextY = node.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
      }

      float dist = Mathf.dst(u.x, u.y, nextX, nextY);

      if (dist <= u.speed * dt) { // 注意这里要乘以 dt
        u.pathIndex++;
        u.velocityDirty = true;
        calculateVelocity(u, dt); // 递归
        return;
      }

      // 计算理想的目标角度
      float targetAngle = Angles.angle(u.x, u.y, nextX, nextY);

      // 应用转向与移动逻辑
      applyMovementLogic(u, targetAngle, dt);

    } else {
      handleFinalApproach(u, dt);
    }
  }

  private void handleFinalApproach(Unit u, float dt) {
    float distToFinal = Mathf.dst(u.x, u.y, u.targetX, u.targetY);

    if (distToFinal > u.speed * dt) {
      float targetAngle = Angles.angle(u.x, u.y, u.targetX, u.targetY);
      applyMovementLogic(u, targetAngle, dt);
    } else {
      stopAndRemove(u);
    }
  }

  /** 核心逻辑：根据 shooting 状态处理转向和速度 */
  private void applyMovementLogic(Unit u, float targetAngle, float dt) {

    // 1. 如果正在射击 (Shooting = true)
    // 这里的逻辑通常是：单位可以任意方向移动（侧滑），不强制转身
    // 或者：单位瞬间转身，忽略转速限制
    if (u.shooting) {
      // 直接将移动角度设为目标角度 (瞬间转身/全向移动)
      u.angle = targetAngle;

      // 全速前进
      u.speedX = Mathf.cosDeg(u.angle) * u.speed;
      u.speedY = Mathf.sinDeg(u.angle) * u.speed;

      // 注意：shooting 时，rotation 通常由 WeaponControl 控制去瞄准敌人
      // 如果这里不管，rotation 会保持之前的样子，这符合"侧向移动"
    }
    // 2. 如果没在射击 (Shooting = false)
    // 必须先转身，角度对准了才能走
    else {
      // 平滑转向当前移动方向
      // rotationSpeed 是 度/tick，乘以 dt 得到当前帧允许转多少度
      u.angle = Angles.moveToward(u.angle, targetAngle, u.rotationSpeed * dt);

      // 只有当朝向基本对准目标时 (例如误差小于 10 度)，才产生位移
      // 否则原地转圈
      if (Angles.within(u.angle, targetAngle, 5f)) {
        // 随着角度越来越准，速度可以从 0 加速到 max (可选，这里简单处理为全速)
        u.speedX = Mathf.cosDeg(u.angle) * u.speed;
        u.speedY = Mathf.sinDeg(u.angle) * u.speed;
      } else {
        // 还在转身中，原地不动
        u.speedX = 0;
        u.speedY = 0;
      }

      // 非射击状态下，渲染朝向通常跟随移动方向
      // u.rotation = u.angle - 90;
      // 注意：你在 UnitProces 里可能还会覆盖这个 rotation
    }

    // 由于我们现在每一帧都在动态计算 angle (MoveToward)，
    // 之前的 velocityDirty 优化在这里不再适用（或者说逻辑变了）。
    // 现在的逻辑是：每一帧都在微调，所以每一帧都要算 sin/cos。
    // 对于后台线程来说，这完全没问题。
    u.velocityDirty = false;
  }

  private void stopAndRemove(Unit u) {
    u.speedX = 0;
    u.speedY = 0;
    u.velocityDirty = false;
    synchronized (WorldData.moveunits) {
      WorldData.moveunits.remove(u);
    }
  }
}
