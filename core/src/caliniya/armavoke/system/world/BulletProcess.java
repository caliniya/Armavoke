package caliniya.armavoke.system.world;

import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Bullet;

public class BulletProcess extends caliniya.armavoke.system.System<BulletProcess> {

  // 1. 接收缓冲区 (接受来自主线程/武器发射的子弹)
  private final Ar<Bullet> pendingBullets = new Ar<>(false, 100);

  // 2. 活跃工作区 (后台私有，用于物理积分和碰撞检测)
  private final Ar<Bullet> activeBullets = new Ar<>(false, 2048);

  // 3. 渲染预备区 (后台生成，准备交给主线程)
  // 我们将计算后的存活子弹放入这里，然后与 WorldData.bullets 交换
  private Ar<Bullet> renderBuffer = new Ar<>(false, 2048);

  @Override
  public BulletProcess init() {
    return super.init(true); // 在后台线程运行
  }

  /**
   * 【对外接口】所有武器开火生成子弹时，调用此方法
   */
  public void addBullet(Bullet b) {
    synchronized (pendingBullets) {
      pendingBullets.add(b);
    }
  }

  public void clearAll() {
    synchronized (pendingBullets) {
      for (int i = 0; i < pendingBullets.size; i++) {
        if (pendingBullets.items[i] != null) ((Bullet)pendingBullets.items[i]).remove();
      }
      pendingBullets.clear();
    }
    
    Object[] activeItems = activeBullets.items;
    for (int i = 0; i < activeBullets.size; i++) {
      if (activeItems[i] != null) {
        ((Bullet)activeItems[i]).remove();
        activeItems[i] = null;
      }
    }
    activeBullets.size = 0;
    
    synchronized (WorldData.bullets) {
        WorldData.bullets.clear();
    }
  }

  @Override
  public void update(float detla) {
    // ==========================================
    // 阶段 1：吸收新子弹
    // ==========================================
    synchronized (pendingBullets) {
      int pSize = pendingBullets.size;
      if (pSize > 0) {
        Object[] pItems = pendingBullets.items;
        for (int i = 0; i < pSize; i++) {
          activeBullets.add((Bullet) pItems[i]);
          pItems[i] = null; // 帮助 GC
        }
        pendingBullets.size = 0; // 清空接收区
      }
    }

    // ==========================================
    // 阶段 2：缓存全局变量
    // ==========================================
    int gridW = WorldData.gridW;
    int gridH = WorldData.gridH;
    Ar<Unit>[] grid = WorldData.unitGrid;
    float chunkSize = WorldData.CHUNK_PIXEL_SIZE;

    Object[] activeItems = activeBullets.items;
    
    // 清空渲染缓冲，准备装填本帧存活的子弹
    renderBuffer.clear();

    // ==========================================
    // 阶段 3：无锁处理活跃子弹
    // ==========================================
    for (int i = 0; i < activeBullets.size; i++) {
      Bullet b = (Bullet) activeItems[i];

      if (b == null || b.type == null) {
        activeItems[i] = activeItems[--activeBullets.size];
        activeItems[activeBullets.size] = null;
        i--;
        continue;
      }

      b.time += 1f;
      if (b.time >= b.type.lifetime) {
        b.type.despawn(b); 
        activeItems[i] = activeItems[--activeBullets.size];
        activeItems[activeBullets.size] = null;
        i--; 
        continue;
      }

      float nextX = b.x + b.velX;
      float nextY = b.y + b.velY;

      Unit hitTarget = null;
      float bHalf = b.type.size / 2f;
      int cx = (int) (nextX / chunkSize);
      int cy = (int) (nextY / chunkSize);

      collisionBlock: 
      for (int dy = -1; dy <= 1; dy++) {
        int ncy = cy + dy;
        if (ncy < 0 || ncy >= gridH) continue;
        int rowOffset = ncy * gridW;

        for (int dx = -1; dx <= 1; dx++) {
          int ncx = cx + dx;
          if (ncx < 0 || ncx >= gridW) continue;

          Ar<Unit> units = grid[rowOffset + ncx];
          if (units == null || units.size == 0) continue;

          Object[] uItems = units.items;
          int uSize = units.size;

          for (int j = 0; j < uSize; j++) {
            Unit u = (Unit) uItems[j];

            if (u == null || u.team == b.team || u.health <= 0) continue;

            float combineHalf = bHalf + (u.size / 2f);
            float diffX = nextX - u.x;
            float diffY = nextY - u.y;

            if (diffX < combineHalf && diffX > -combineHalf &&
                diffY < combineHalf && diffY > -combineHalf) {
              hitTarget = u;
              break collisionBlock;
            }
          }
        }
      }

      if (hitTarget != null) {
        b.x = nextX;
        b.y = nextY;
        b.type.hit(b, hitTarget);
        
        activeItems[i] = activeItems[--activeBullets.size];
        activeItems[activeBullets.size] = null;
        i--;
      } else {
        b.x = nextX;
        b.y = nextY;
        b.type.update(b);
        
        // 【关键改动】：如果子弹没有被销毁(依然存活)，将它加入本帧的渲染缓冲
        renderBuffer.add(b);
      }
    }

    // ==========================================
    // 阶段 4：极速指针交换 (向主线程提交数据)
    // ==========================================
    synchronized (WorldData.bullets) {
        // 保存原来主线程正在用的数组
        Ar<Bullet> oldGlobal = WorldData.bullets;
        
        // 瞬间将计算好的缓冲队列替换给主线程
        WorldData.bullets = renderBuffer;
        
        // 把原来被替换下来的数组拿回来，作为下一帧后台计算的渲染缓冲
        // 这样就不需要不停地 new Ar<Bullet>() 了
        renderBuffer = oldGlobal; 
    }
  }
}