package caliniya.armavoke.system.world;

import arc.Core;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.tool.EntityAr;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Bullet;

/** 子弹处理系统。 管理子弹生命周期、移动、碰撞及渲染数据同步，后台线程运行，双缓冲保证线程安全。 */
public class BulletProcess extends caliniya.armavoke.system.System<BulletProcess> {

  /** 双缓冲专用锁。 逻辑线程交换 WorldData.bullets 与 renderBuffer 引用时， 必须用此固定锁对象，避免锁在不同实例上导致互斥失效。 */
  public static final Object BULLET_LOCK = new Object();

  // ==================== ID 生成系统 ====================

  /** 子弹ID计数器（从1开始，0表示无效ID） 使用 volatile 保证多线程可见性 */
  private static volatile int nextBulletId = 1;

  /** 空闲ID池（用于回收复用） 存储被销毁子弹的ID，供新子弹复用 */
  private static final Ar<Integer> freeIds = new Ar<>(false, 256);

  /**
   * 为子弹分配唯一ID（线程安全）
   *
   * @return 新的唯一ID
   */
  private static synchronized int allocateBulletId() {
    // 优先从空闲池取
    if (!freeIds.isEmpty()) {
      return freeIds.remove(freeIds.size - 1);
    }
    // 没有空闲ID，分配新ID
    return nextBulletId++;
  }

  /**
   * 回收子弹ID（供复用）
   *
   * @param id 需要回收的ID
   */
  private static synchronized void recycleBulletId(int id) {
    if (id > 0) {
      freeIds.add(id);
    }
  }

  // ==================== 数据存储 ====================

  /** 待处理子弹队列（线程安全） */
  private final EntityAr<Bullet> pendingBullets = new EntityAr<>(b -> b.id);

  /** 活跃子弹列表（逻辑线程专用） */
  private final EntityAr<Bullet> activeBullets = new EntityAr<>(b -> b.id);

  /** 渲染缓冲区（与 WorldData.bullets 交换） */
  private EntityAr<Bullet> renderBuffer = new EntityAr<>(b -> b.id);

  /** 待删除列表（避免遍历时修改） */
  private final Ar<Bullet> toRemove = new Ar<>(false, 256);

  @Override
  public BulletProcess init() {
    return super.init(true);
  }

  /** 添加子弹（线程安全） 会自动为子弹分配唯一ID */
  public void addBullet(Bullet b) {
    if (b == null) return;

    // 为子弹分配ID（如果还没有）
    if (b.id <= 0) {
      b.id = allocateBulletId();
    }

    synchronized (pendingBullets) {
      pendingBullets.add(b);
    }
  }

  /** 批量添加子弹 */
  public void addBullets(Bullet... bullets) {
    if (bullets == null || bullets.length == 0) return;

    for (Bullet b : bullets) {
      if (b != null && b.id <= 0) {
        b.id = allocateBulletId();
      }
    }

    synchronized (pendingBullets) {
      pendingBullets.add(bullets);
    }
  }

  /** 清除所有子弹并重置 */
  public void clearAll() {
    // 回收所有子弹ID
    synchronized (pendingBullets) {
      pendingBullets.each(
          b -> {
            if (b != null) {
              b.remove();
              recycleBulletId(b.id);
            }
          });
      pendingBullets.clear();
    }

    // 回收活跃子弹的ID
    activeBullets.each(
        b -> {
          if (b != null) {
            b.remove();
            recycleBulletId(b.id);
          }
        });
    activeBullets.clear();
    renderBuffer.clear();

    synchronized (BULLET_LOCK) {
      // 回收 WorldData.bullets 中的子弹ID
      WorldData.bullets.each(
          b -> {
            if (b != null) recycleBulletId(b.id);
          });
      WorldData.bullets.clear();
    }
  }

  @Override
  public void update(float delta) {
    // 合并待处理子弹
    synchronized (pendingBullets) {
      // 注意：这里传入的是数组，需要确保加锁安全
      pendingBullets.each(
          b -> {
            if (b != null) {
              // 确保子弹有ID（如果外部没有分配）
              if (b.id <= 0) {
                b.id = allocateBulletId();
              }
              activeBullets.add(b);
            }
          });

      pendingBullets.clear();
    }

    toRemove.clear();

    // 更新所有子弹
    activeBullets.each(
        b -> {
          b.time += 1f;
          if (b.time >= b.type.lifetime) {
            b.type.despawn(b);
            toRemove.add(b);
            return;
          }

          float nextX = b.x + b.velX * delta;
          float nextY = b.y + b.velY * delta;
          b.x = nextX;
          b.y = nextY;
          activeBullets.move(b, nextX, nextY);

          Entities.closestEnemy(
              b.team,
              nextX,
              nextY,
              b.type.size,
              e -> {
                b.type.hit(b, e);
                toRemove.add(b);
              });
        });

    // 批量删除（回收ID）
    toRemove.each(
        b -> {
          activeBullets.remove(b);
          recycleBulletId(b.id); // 回收ID供复用
        });
    toRemove.clear();

    // 更新渲染缓冲区并交换
    renderBuffer.clear();
    // 批量添加所有活跃子弹到渲染缓冲区
    activeBullets.each(renderBuffer::add);

    synchronized (BULLET_LOCK) {
      EntityAr<Bullet> temp = WorldData.bullets;
      WorldData.bullets = renderBuffer;
      renderBuffer = temp;
    }
  }

  /** 获取当前子弹ID池状态 */
  public void debug() {
    Log.info(
        "Bullets: active="
            + activeBullets.size()
            + ", pending="
            + pendingBullets.size()
            + ", render="
            + renderBuffer.size()
            + ", nextId="
            + nextBulletId
            + ", freeIds="
            + freeIds.size);
  }
}
