package caliniya.armavoke.game.data;

import arc.func.Cons;
import arc.math.Mathf;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Unit;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TeamData {
  public final TeamTypes team;

  /** 该团队下的所有单位列表 (全局) */
  public Ar<Unit> units = new Ar<>();

  /** 空间划分网格 (Per-Team Spatial Grid) */
  public Ar<Unit>[] unitGrid;

  /**
   * 读写锁，保护 unitGrid 的并发访问。
   *
   * <ul>
   *   <li>读锁：{@link #find} / {@link #get} — 允许多个线程同时搜索。
   *   <li>写锁：{@link #updateChunk} — 单位增删/移动时独占。
   * </ul>
   */
  private final ReentrantReadWriteLock gridLock = new ReentrantReadWriteLock();

  @SuppressWarnings("unchecked")
  public TeamData(TeamTypes team) {
    this.team = team;
    initGrid();
  }

  /** 初始化/重置网格 (需在地图加载后调用) */
  @SuppressWarnings("unchecked")
  public void initGrid() {
    gridLock.writeLock().lock();
    try {
      int w = WorldData.gridW;
      int h = WorldData.gridH;
      int total = w * h;

      this.unitGrid = new Ar[total];
      for (int i = 0; i < total; i++) {
        this.unitGrid[i] = new Ar<>(8);
      }
    } finally {
      gridLock.writeLock().unlock();
    }
  }

  /**
   * 获取指定矩形区域内的本团队单位（读锁保护）。
   */
  public void get(float minX, float minY, float maxX, float maxY, Ar<Unit> output) {
    gridLock.readLock().lock();
    try {
      if (unitGrid == null) return;

      int startX = (int) (minX / WorldData.CHUNK_PIXEL_SIZE);
      int startY = (int) (minY / WorldData.CHUNK_PIXEL_SIZE);
      int endX = (int) (maxX / WorldData.CHUNK_PIXEL_SIZE);
      int endY = (int) (maxY / WorldData.CHUNK_PIXEL_SIZE);

      startX = Mathf.clamp(startX, 0, WorldData.gridW - 1);
      startY = Mathf.clamp(startY, 0, WorldData.gridH - 1);
      endX = Mathf.clamp(endX, 0, WorldData.gridW - 1);
      endY = Mathf.clamp(endY, 0, WorldData.gridH - 1);

      for (int y = startY; y <= endY; y++) {
        for (int x = startX; x <= endX; x++) {
          int index = y * WorldData.gridW + x;
          Ar<Unit> chunkUnits = unitGrid[index];

          for (int i = 0; i < chunkUnits.size; i++) {
            Unit u = chunkUnits.get(i);
            if (u.x >= minX && u.x <= maxX && u.y >= minY && u.y <= maxY) {
              output.add(u);
            }
          }
        }
      }
    } finally {
      gridLock.readLock().unlock();
    }
  }

  /**
   * 在指定圆形范围内查找本团队的单位（读锁保护）。
   */
  public void find(float x, float y, float radius, Cons<Unit> consumer) {
    gridLock.readLock().lock();
    try {
      if (unitGrid == null) return;

      float minX = x - radius;
      float minY = y - radius;
      float maxX = x + radius;
      float maxY = y + radius;

      int startX = (int) (minX / WorldData.CHUNK_PIXEL_SIZE);
      int startY = (int) (minY / WorldData.CHUNK_PIXEL_SIZE);
      int endX = (int) (maxX / WorldData.CHUNK_PIXEL_SIZE);
      int endY = (int) (maxY / WorldData.CHUNK_PIXEL_SIZE);

      startX = Mathf.clamp(startX, 0, WorldData.gridW - 1);
      startY = Mathf.clamp(startY, 0, WorldData.gridH - 1);
      endX = Mathf.clamp(endX, 0, WorldData.gridW - 1);
      endY = Mathf.clamp(endY, 0, WorldData.gridH - 1);

      float r2 = radius * radius;

      for (int gy = startY; gy <= endY; gy++) {
        for (int gx = startX; gx <= endX; gx++) {
          int index = gy * WorldData.gridW + gx;
          Ar<Unit> chunkUnits = unitGrid[index];

          for (int i = 0; i < chunkUnits.size; i++) {
            Unit u = chunkUnits.get(i);

            if (u == null || u.health <= 0) continue;
            if (u.x < minX || u.x > maxX || u.y < minY || u.y > maxY) continue;

            if (Mathf.dst2(x, y, u.x, u.y) <= r2) {
              consumer.get(u);
            }
          }
        }
      }
    } finally {
      gridLock.readLock().unlock();
    }
  }

  /**
   * 更新单位在团队空间网格中的位置（写锁保护）。
   */
  public void updateChunk(Unit u, int oldIndex, int newIndex) {
    gridLock.writeLock().lock();
    try {
      if (unitGrid == null) return;

      if (oldIndex != -1 && oldIndex < unitGrid.length) {
        unitGrid[oldIndex].remove(u);
      }

      if (newIndex >= 0 && newIndex < unitGrid.length) {
        unitGrid[newIndex].add(u);
      }
    } finally {
      gridLock.writeLock().unlock();
    }
  }
}
