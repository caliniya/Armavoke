package caliniya.armavoke.system.world;

import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.util.Log;
import arc.util.Time;
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

      if (!u.pathed) {
        calculatePath(u);
        u.pathed = true;
        u.velocityDirty = true;
      }

      calculateVelocity(u);
    }
  }

  private void calculatePath(Unit u) {
    int sx = (int) (u.x / WorldData.TILE_SIZE);
    int sy = (int) (u.y / WorldData.TILE_SIZE);
    int tx = (int) (u.targetX / WorldData.TILE_SIZE);
    int ty = (int) (u.targetY / WorldData.TILE_SIZE);

    if (sx != tx || sy != ty) {
      u.path = RouteData.findPath(sx, sy, tx, ty, 4, 0);

      // 第一个点(起点)不要去
      if (u.path != null && !u.path.isEmpty()) {
        u.path.remove(0);
      }
      

      u.pathIndex = 0;

      // 如果移除起点后路径变空了（说明起点和终点挨得很近或者逻辑重叠），
      // 下面的 isEmpty 检查会处理它，让单位直接由 calculateVelocity 接管去往 targetX/Y
      if (u.path == null || u.path.isEmpty()) {
        u.pathFindCooldown = 60f;
        u.speedX = 0;
        u.speedY = 0;

        // 注意：这里如果只是因为移除了起点导致为空，不代表"寻路失败"，
        // 而是代表"剩下的路就是直线走过去"，所以这里不要急着 stopAndRemove。
        // 下面的 calculateVelocity 会检测到 empty 并调用 handleFinalApproach，逻辑是闭环的。

        // 只有当 findPath 本身返回 null (不可达) 时才视为失败
        if (u.path == null) {
          stopAndRemove(u);
        }
      }
    } else {
      if (u.path != null) u.path.clear();
      u.velocityDirty = true;
    }
  }

  private void calculateVelocity(Unit u) {
    // 如果路径为空（或者被移除了第一个点后变为空），直接进入最后逼近逻辑
    if (u.path == null || u.path.isEmpty()) {
      handleFinalApproach(u);
      return;
    }

    if (u.pathIndex < u.path.size) {

      float nextX, nextY;

      // 如果是路径列表的最后一个点 -> 使用精确点击坐标
      if (u.pathIndex == u.path.size - 1) {
        nextX = u.targetX;
        nextY = u.targetY;
      }
      // 否则 -> 使用网格中心坐标
      else {
        Point2 node = u.path.get(u.pathIndex);
        nextX = node.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
        nextY = node.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
      }

      float dist = Mathf.dst(u.x, u.y, nextX, nextY);

      if (dist <= u.speed) {
        u.pathIndex++;
        u.velocityDirty = true;
        calculateVelocity(u); // 递归
        return;
      }

      if (u.velocityDirty) {
        u.angle = Angles.angle(u.x, u.y, nextX, nextY);
        u.speedX = Mathf.cosDeg(u.angle) * u.speed;
        u.speedY = Mathf.sinDeg(u.angle) * u.speed;
        u.velocityDirty = false;
      }
    } else {
      handleFinalApproach(u);
    }
  }

  private void handleFinalApproach(Unit u) {
    float distToFinal = Mathf.dst(u.x, u.y, u.targetX, u.targetY);

    if (distToFinal > u.speed) {
      if (u.velocityDirty && distToFinal > 0.1f) {
        u.angle = Angles.angle(u.x, u.y, u.targetX, u.targetY);
        u.speedX = Mathf.cosDeg(u.angle) * u.speed;
        u.speedY = Mathf.sinDeg(u.angle) * u.speed;
        u.velocityDirty = false;
      }
    } else {
      stopAndRemove(u);
    }
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
