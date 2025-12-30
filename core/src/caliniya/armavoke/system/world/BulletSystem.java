package caliniya.armavoke.system.world;

import arc.math.geom.Rect;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;
import caliniya.armavoke.type.Bullet;

public class BulletSystem extends BasicSystem<BulletSystem> {

  // 碰撞检测辅助矩形
  private final Rect hitRect = new Rect();

  @Override
  public BulletSystem init() {
    return super.init(true);
  }

  @Override
  public void update() {
    // 遍历所有子弹
    // 注意：由于 update 过程中子弹可能会销毁(remove)，
    // 我们通常倒序遍历，或者使用 remove 安全的迭代方式
    //暂时先这样
    Ar<Bullet> list = WorldData.bullets;

    for (int i = list.size - 1; i >= 0; i--) {
      Bullet b = list.get(i);
      updateBullet(b);
    }
  }

  private void updateBullet(Bullet b) {
    // 生命周期检查
    b.time += 1f;
    if (b.time >= b.type.lifetime) {
      b.type.despawn(b);
      return;
    }

    // 移动 (简单的欧拉积分)
    float nextX = b.x + b.velX;
    float nextY = b.y + b.velY;

    // 碰撞检测
    // 我们需要检测 (nextX, nextY) 是否撞到了敌人
    Unit hitTarget = checkCollision(b, nextX, nextY);

    if (hitTarget != null) {
      // 命中单位
      b.x = nextX;
      b.y = nextY;
      b.type.hit(b, hitTarget);
    } else if (WorldData.world.isSolid(
        (int) (nextX / WorldData.TILE_SIZE), (int) (nextY / WorldData.TILE_SIZE))) {
      // 命中墙壁
      b.x = nextX;
      b.y = nextY;
      b.type.despawn(b);
    } else {
      // 未命中，正常移动
      b.x = nextX;
      b.y = nextY;

      // 调用类型特定的更新逻辑 (特效等)
      b.type.update(b);
    }
  }

  /** 基于网格的 AABB 碰撞检测 */
  private Unit checkCollision(Bullet b, float checkX, float checkY) {
    // 计算子弹的 AABB (在 checkX, checkY 位置)
    float bHalfW = b.type.hitW;
    float bHalfH = b.type.hitH;

    float bMinX = checkX - bHalfW;
    float bMinY = checkY - bHalfH;
    float bMaxX = checkX + bHalfW;
    float bMaxY = checkY + bHalfH;

    // 空间网格查询
    int cx = (int) (checkX / WorldData.CHUNK_PIXEL_SIZE);
    int cy = (int) (checkY / WorldData.CHUNK_PIXEL_SIZE);

    // 3x3 邻居检查
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        int ncx = cx + dx;
        int ncy = cy + dy;

        if (ncx < 0 || ncx >= WorldData.gridW || ncy < 0 || ncy >= WorldData.gridH) continue;

        int index = ncy * WorldData.gridW + ncx;
        Ar<Unit> units = WorldData.unitGrid[index];

        if (units == null || units.isEmpty()) continue;

        for (int i = 0; i < units.size; i++) {
          Unit u = units.get(i);

          if (u == null || u.health <= 0) continue;
          if (u.team == b.team) continue;

          // AABB 碰撞检测
          // Unit 的体积是 w * h，中心在 u.x, u.y
          float uHalfW = u.w / 2f;
          float uHalfH = u.h / 2f;

          float uMinX = u.x - uHalfW;
          float uMaxX = u.x + uHalfW;
          float uMinY = u.y - uHalfH;
          float uMaxY = u.y + uHalfH;

          // 矩形重叠判定：两个矩形相交当且仅当它们在 X 轴和 Y 轴上的投影都重叠
          // 逻辑：!(b在u左边 || b在u右边 || b在u上面 || b在u下面)
          if (bMaxX >= uMinX && bMinX <= uMaxX && bMaxY >= uMinY && bMinY <= uMaxY) {
            return u;
          }
        }
      }
    }

    return null;
  }
}
