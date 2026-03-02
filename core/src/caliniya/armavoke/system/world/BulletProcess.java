package caliniya.armavoke.system.world;

import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Bullet;

/**
 * 子弹处理系统。
 *
 * <p>负责管理游戏中所有子弹的生命周期、移动逻辑、碰撞检测以及渲染数据的同步。 该系统在后台线程中运行，通过双缓冲机制确保渲染线程能够安全地读取子弹数据。
 */
public class BulletProcess extends caliniya.armavoke.system.System<BulletProcess> {

  /**
   * 待处理子弹列表。
   *
   * <p>用于临时存储从外部线程添加的子弹对象。 使用同步锁机制保证线程安全。
   */
  private final Ar<Bullet> pendingBullets = new Ar<>(false, 100);

  /**
   * 活跃子弹列表。
   *
   * <p>存储当前正在游戏中运行的所有子弹。 仅在逻辑线程中被访问和修改。
   */
  private final Ar<Bullet> activeBullets = new Ar<>(false, 2048);

  /**
   * 渲染缓冲区。
   *
   * <p>每一帧逻辑更新结束后，将需要渲染的子弹存入此缓冲区， 随后与 {@link WorldData#bullets} 交换，供渲染线程读取。
   */
  private Ar<Bullet> renderBuffer = new Ar<>(false, 2048);

  /**
   * 初始化子弹处理系统。
   *
   * @return 返回当前系统实例
   */
  @Override
  public BulletProcess init() {
    return super.init(true); // 在后台线程运行
  }

  /**
   * 【对外接口】添加一颗新子弹到世界中。
   *
   * <p>该方法是线程安全的，可由主线程或其他逻辑线程调用。 子弹不会立即加入活跃列表，而是先存入待处理队列，在下一帧更新时合并。
   *
   * @param b 需要添加的子弹对象
   */
  public void addBullet(Bullet b) {
    synchronized (pendingBullets) {
      pendingBullets.add(b);
    }
  }

  /**
   * 清除所有子弹并重置系统状态。
   *
   * <p>该方法会清理待处理队列、活跃列表以及全局渲染数据中的所有子弹， 并调用子弹自身的移除逻辑。
   */
  public void clearAll() {
    // 清理待处理队列
    synchronized (pendingBullets) {
      for (int i = 0; i < pendingBullets.size; i++) {
        if (pendingBullets.items[i] != null) ((Bullet) pendingBullets.items[i]).remove();
      }
      pendingBullets.clear();
    }

    // 清理活跃列表
    Object[] activeItems = activeBullets.items;
    for (int i = 0; i < activeBullets.size; i++) {
      if (activeItems[i] != null) {
        ((Bullet) activeItems[i]).remove();
        activeItems[i] = null;
      }
    }
    activeBullets.size = 0;

    // 清理全局渲染引用
    synchronized (WorldData.bullets) {
      WorldData.bullets.clear();
    }
  }

  /**
   * 每帧更新逻辑。
   *
   * <p>主要执行以下步骤：
   *
   * <ol>
   *   <li>将待处理子弹合并到活跃列表
   *   <li>更新子弹存活时间，移除过期子弹
   *   <li>计算子弹下一帧位置并进行碰撞检测
   *   <li>处理碰撞事件（造成伤害等）
   *   <li>更新渲染缓冲区
   * </ol>
   *
   * @param detla 帧间隔时间（秒）
   */
  @Override
  public void update(float detla) {
    //debug(detla);
    // 1. 合并待处理子弹到活跃列表
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

    // 获取世界网格数据用于碰撞检测
    int gridW = WorldData.gridW;
    int gridH = WorldData.gridH;
    Ar<Unit>[] grid = WorldData.unitGrid;
    float chunkSize = WorldData.CHUNK_PIXEL_SIZE;

    Object[] activeItems = activeBullets.items;

    renderBuffer.clear();

    // 2. 遍历更新所有活跃子弹
    for (int i = 0; i < activeBullets.size; i++) {
      Bullet b = (Bullet) activeItems[i];

      // 空值或类型检查
      if (b == null || b.type == null) {
        activeItems[i] = activeItems[--activeBullets.size];
        activeItems[activeBullets.size] = null;
        i--;
        continue;
      }

      // 生命周期检测
      b.time += 1f;
      if (b.time >= b.type.lifetime) {
        b.type.despawn(b);
        activeItems[i] = activeItems[--activeBullets.size];
        activeItems[activeBullets.size] = null;
        i--;
        continue;
      }

      // 计算下一位置
      float nextX = b.x + b.velX * detla;
      float nextY = b.y + b.velY * detla;

      // 3. 碰撞检测
      Unit hitTarget = null;
      float bHalf = b.type.size / 2f;
      int cx = (int) (nextX / chunkSize);
      int cy = (int) (nextY / chunkSize);

      // 仅检测子弹所在的区块及其周围 3x3 范围
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

            // 过滤无效目标：空值、同队、已死亡
            if (u == null || u.team == b.team || u.health <= 0) continue;

            // 盒式碰撞检测
            float combineHalf = bHalf + (u.size / 2f);
            float diffX = nextX - u.x;
            float diffY = nextY - u.y;

            if (diffX < combineHalf
                && diffX > -combineHalf
                && diffY < combineHalf
                && diffY > -combineHalf) {
              hitTarget = u;
              break collisionBlock;
            }
          }
        }
      }

      // 4. 处理碰撞结果
      if (hitTarget != null) {
        b.x = nextX;
        b.y = nextY;
        b.type.hit(b, hitTarget);

        // 命中后移除子弹
        activeItems[i] = activeItems[--activeBullets.size];
        activeItems[activeBullets.size] = null;
        i--;
      } else {
        // 未命中，更新位置并存入渲染缓冲
        b.x = nextX;
        b.y = nextY;
        b.type.update(b);

        renderBuffer.add(b);
      }
    }

    // 5. 交换渲染缓冲区
    synchronized (WorldData.bullets) {
      Ar<Bullet> temp = WorldData.bullets;

      WorldData.bullets = renderBuffer;

      renderBuffer = temp;
    }
  }
  
  public void debug(float detla) {
  	Log.info(detla);
  }
  
}
