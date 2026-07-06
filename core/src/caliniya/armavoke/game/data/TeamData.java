package caliniya.armavoke.game.data;

import arc.func.Cons;
import arc.math.Mathf;
import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.base.game.Entity;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TeamData {
  public final TeamTypes team;

  /** 该团队下的所有实体列表 (全局) */
  public Ar<Entity> entities = new Ar<>();

  /** 空间划分网格 (Per-Team Spatial Grid) */
  public Ar<Entity>[] entityGrid;

  private final ReentrantReadWriteLock gridLock = new ReentrantReadWriteLock();

  @SuppressWarnings("unchecked")
  public TeamData(TeamTypes team) {
    this.team = team;
    initGrid();
  }

  @SuppressWarnings("unchecked")
  public void initGrid() {
    gridLock.writeLock().lock();
    try {
      int w = WorldData.gridW;
      int h = WorldData.gridH;
      int total = w * h;

      this.entityGrid = new Ar[total];
      for (int i = 0; i < total; i++) {
        this.entityGrid[i] = new Ar<>(8);
      }
    } finally {
      gridLock.writeLock().unlock();
    }
  }

  public void get(float minX, float minY, float maxX, float maxY, Ar<Entity> output) {
    gridLock.readLock().lock();
    try {
      if (entityGrid == null) return;

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
          Ar<Entity> chunk = entityGrid[index];

          for (int i = 0; i < chunk.size; i++) {
            Entity e = chunk.get(i);
            if (e.x >= minX && e.x <= maxX && e.y >= minY && e.y <= maxY) {
              output.add(e);
            }
          }
        }
      }
    } finally {
      gridLock.readLock().unlock();
    }
  }

  /** 在指定圆形范围内查找本团队的实体（读锁保护）。 */
  public void find(float x, float y, float radius, Cons<Entity> consumer) {
    gridLock.readLock().lock();
    try {
      if (entityGrid == null) return;

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
          Ar<Entity> chunk = entityGrid[index];

          for (int i = 0; i < chunk.size; i++) {
            Entity e = chunk.get(i);

            if (e == null || e.health <= 0) continue;
            if (e.x < minX || e.x > maxX || e.y < minY || e.y > maxY) continue;

            if (Mathf.dst2(x, y, e.x, e.y) <= r2) {
              consumer.get(e);
            }
          }
        }
      }
    } finally {
      gridLock.readLock().unlock();
    }
  }

  /** 更新实体在团队空间网格中的位置（写锁保护）。 */
  public void updateChunk(Entity e, int oldIndex, int newIndex) {
    gridLock.writeLock().lock();
    try {
      if (entityGrid == null) return;

      if (oldIndex != -1 && oldIndex < entityGrid.length) {
        entityGrid[oldIndex].remove(e);
      }

      if (newIndex >= 0 && newIndex < entityGrid.length) {
        entityGrid[newIndex].add(e);
      }
    } finally {
      gridLock.writeLock().unlock();
    }
  }
}
