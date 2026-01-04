package caliniya.armavoke.system.world;

import arc.math.geom.Rect;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.core.Units;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;
import caliniya.armavoke.type.Bullet;

//子弹处理
public class BulletSystem extends BasicSystem<BulletSystem> {
  
  Ar<Bullet> list = new Ar<>(false , 1000);
  
  // 碰撞检测辅助矩形
  //private final Rect hitRect = new Rect();

  @Override
  public BulletSystem init() {
    return super.init(true);
  }

  @Override
  public void update() {
    // 遍历所有子弹
    list.clear();
    synchronized(WorldData.bullets){
      list.addAll(WorldData.bullets);
    }

    for (int i = 0; i < list.size; i++) {
      Bullet b = list.get(i);
      if(b == null || b.type == null) continue;
      updateBullet(b);
    }
  }

  /** 基于网格的 AABB 碰撞检测 */
  private void updateBullet(Bullet b) {
    b.time += 1f;
    if (b.time >= b.type.lifetime) {
      b.type.despawn(b);
      return;
    }

    float nextX = b.x + b.velX;
    float nextY = b.y + b.velY;

    // 碰撞检测 (只检测单位，移除了墙壁检测)
    Unit hitTarget = checkCollision(b, nextX, nextY);

    if (hitTarget != null) {
      b.x = nextX;
      b.y = nextY;
      b.type.hit(b, hitTarget);
    } else {
      b.x = nextX;
      b.y = nextY;
      b.type.update(b);
    }
  }

  private Unit checkCollision(Bullet b, float checkX, float checkY) {
    float bHalf = b.type.size / 2f;
    float bMinX = checkX - bHalf;
    float bMinY = checkY - bHalf;
    float bMaxX = checkX + bHalf;
    float bMaxY = checkY + bHalf;

    int cx = (int) (checkX / WorldData.CHUNK_PIXEL_SIZE);
    int cy = (int) (checkY / WorldData.CHUNK_PIXEL_SIZE);

    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        int ncx = cx + dx;
        int ncy = cy + dy;

        if (ncx < 0 || ncx >= WorldData.gridW || ncy < 0 || ncy >= WorldData.gridH) continue;

        Ar<Unit> units = WorldData.unitGrid[ncy * WorldData.gridW + ncx];
        if (units == null || units.isEmpty()) continue;

        for (int i = 0; i < units.size; i++) {
          Unit u = units.get(i);

          if (u == null || u.health <= 0) continue;
          if (u.team == b.team) continue;

          // 两个正方形碰撞检测
          float uHalf = u.size / 2f;
          if (Math.abs(checkX - u.x) < (bHalf + uHalf) && 
              Math.abs(checkY - u.y) < (bHalf + uHalf)) {
              return u;
          }
        }
      }
    }
    return null;
  }
}
