package caliniya.armavoke.system.world;

import arc.Core;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Entities;
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
   * 子弹双缓冲的专用锁对象。
   *
   * <p><b>关键：</b>逻辑线程会交换 {@link WorldData#bullets} 与 {@link #renderBuffer} 的引用， 所以绝对不能用 {@code
   * synchronized(WorldData.bullets)} 加锁——那样锁的是"当前指向的对象实例"， 交换后两个线程会锁在不同实例上，互斥失效，导致渲染读到正在被清空/重填的缓冲 →
   * 子弹闪烁。
   *
   * <p>因此这里用一个永不变化的 final 锁对象，逻辑线程（swap / clearAll）与渲染线程 都统一锁它。
   */
  public static final Object BULLET_LOCK = new Object();

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
        if (pendingBullets.items[i] != null) (pendingBullets.items[i]).remove();
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

    // 清理全局渲染引用（用固定锁，不锁会被交换的 WorldData.bullets 引用）
    synchronized (BULLET_LOCK) {
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
    // 1. 合并待处理子弹
    synchronized (pendingBullets) {
      activeBullets.addAll(pendingBullets);
      pendingBullets.clear();
    }
    activeBullets.each(
        b -> {
          b.time += 1f;
          if (b.time >= b.type.lifetime) {
            b.type.despawn(b);
            activeBullets.remove(b);
            return;
          }
          float nextX = b.x + b.velX * detla;
          float nextY = b.y + b.velY * detla;
          b.x = nextX;
                b.y = nextY;
          Entities.nearbyEnemies(
              b.team,
              nextX,
              nextY,
              b.type.size,
              e -> {
                b.type.hit(b, e);;
                activeBullets.remove(b);
              });
        });
    renderBuffer.addAll(activeBullets);

    // 5. 交换渲染缓冲区（用固定锁对象，保证与渲染线程互斥）
    synchronized (BULLET_LOCK) {
      Ar<Bullet> temp = WorldData.bullets;
      WorldData.bullets = renderBuffer;
      renderBuffer = temp;
    }
  }

  public void debug(float detla) {
    Log.info(detla);
  }
}
