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

    for (int i = 0; i < processList.size; ++i) {
      Unit u = processList.get(i);

      if (u == null || u.health <= 0) {
        synchronized (WorldData.moveunits) {
          WorldData.moveunits.remove(u);
        }
        continue;
      }

      // 1. 路径请求 (只在无路径时计算一次)
      if (!u.pathed) {
        calculatePath(u);
        u.pathed = true;
      }

      // 2. 向量计算 (每一帧都根据当前位置计算期望速度向量)
      calculateVelocityVector(u);
    }
  }

  private void calculatePath(Unit u) {
    int sx = (int) (u.x / WorldData.TILE_SIZE);
    int sy = (int) (u.y / WorldData.TILE_SIZE);
    int tx = (int) (u.targetX / WorldData.TILE_SIZE);
    int ty = (int) (u.targetY / WorldData.TILE_SIZE);

    if (sx != tx || sy != ty) {
      // 这里的 2, 1 暂时硬编码，未来应从 u.type 读取
      u.path = RouteData.findPath(sx, sy, tx, ty, 2, 1);

      if (u.path != null && !u.path.isEmpty()) {
        u.path.remove(0); // 移除起点，防止回退抖动
      }
      u.pathIndex = 0;

      if (u.path == null) {
        stopAndRemove(u);
      }
    } else {
      if (u.path != null) u.path.clear();
    }
  }
  
  
  private void calculateVelocityVector(Unit u) {
    float nextX, nextY;

    // 确定当前目标点
    if (u.path == null || u.path.isEmpty()) {
      // 无路径：直接走直线去最终目标
      nextX = u.targetX;
      nextY = u.targetY;
    } else {
      // 有路径：去往当前路点
      if (u.pathIndex >= u.path.size) {
        // 容错：索引越界视同去往最终目标
        nextX = u.targetX;
        nextY = u.targetY;
      } else if (u.pathIndex == u.path.size - 1) {
        // 最后一个点使用精确坐标
        nextX = u.targetX;
        nextY = u.targetY;
      } else {
        // 中间点使用网格中心
        Point2 node = u.path.get(u.pathIndex);
        nextX = node.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
        nextY = node.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
      }
    }

    // 计算期望速度向量
    // 这里我们只告诉 Unit "应该往哪个方向全速前进"
    float dist = Mathf.dst(u.x, u.y, nextX, nextY);
    // 这里的阈值可以是一个很小的常量
    if (dist < 2f) {
      u.speedX = 0;
      u.speedY = 0;
    } else {
      float targetAngle = Angles.angle(u.x, u.y, nextX, nextY);
      u.speedX = Mathf.cosDeg(targetAngle) * u.speed;
      u.speedY = Mathf.sinDeg(targetAngle) * u.speed;
    }
  }

  private void stopAndRemove(Unit u) {
    u.speedX = 0;
    u.speedY = 0;
    synchronized (WorldData.moveunits) {
      WorldData.moveunits.remove(u);
    }
  }
}
