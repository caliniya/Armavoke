package caliniya.armavoke.game.data;

import arc.Events;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.*;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.system.render.MapRender;
import caliniya.armavoke.world.World;
import arc.math.Mathf;

public class WorldData {
  public static World world;

  // 全局单位列表 (用于逻辑更新)
  public static Ar<Unit> units = new Ar<>(10);
  // 有移动目标的单位
  public static Ar<Unit> moveunits = new Ar<>(5);

  // --- 空间划分网格相关 ---
  // 每个区块包含的瓦片数量 (32x32个地块)
  public static final int CHUNK_SIZE = 32;
  // 单个地块的像素大小
  public static final int TILE_SIZE = 32;
  // 单个区块的像素大小 (32 * 32 = 1024)
  public static final int CHUNK_PIXEL_SIZE = CHUNK_SIZE * TILE_SIZE;

  // 网格的宽和高 (以区块为单位)
  public static int gridW, gridH;

  // 存储分区的数组，每个元素是一个单位列表
  // 使用数组而不是Ar<Ar<Unit>>是为了访问速度略微快一点
  public static Ar<Unit>[] unitGrid;

  private WorldData() {}

  @SuppressWarnings("unchecked")
  public static void initWorld() {
    world = new World();
    world.test = true;
    world.init();

    // 1. 初始化网格尺寸
    // 即使地图大小不能整除32，也要向上取整多算一个格子，防止越界
    gridW = Mathf.ceil((float) world.W / CHUNK_SIZE);
    gridH = Mathf.ceil((float) world.H / CHUNK_SIZE);

    // 2. 初始化网格数组
    int totalChunks = gridW * gridH;
    unitGrid = new Ar[totalChunks];
    for (int i = 0; i < totalChunks; i++) {
      unitGrid[i] = new Ar<>(16); // 预设每个格子大概有16个单位，减少扩容开销
    }
    Teams.init();
  }

  public static void clearunits() {
    if (units != null) {
      units.each(
        unit -> {
          unit.reset();
        }
      );
      units.clear();
    }

    // 清理网格中的残留引用
    if (unitGrid != null) {
      for (Ar<Unit> list : unitGrid) {
        list.clear();
      }
    }
  }

  /** 根据像素坐标计算网格索引 */
  public static int getChunkIndex(float x, float y) {
    // 将像素坐标转换为区块坐标
    int cx = (int) (x / CHUNK_PIXEL_SIZE);
    int cy = (int) (y / CHUNK_PIXEL_SIZE);

    // 边界限制，防止单位跑出地图外导致数组越界
    cx = Mathf.clamp(cx, 0, gridW - 1);
    cy = Mathf.clamp(cy, 0, gridH - 1);

    return cy * gridW + cx;
  }

  /** 获取指定像素位置所在的单位列表 */
  public static Ar<Unit> getUnitsAtChunk(float x, float y) {
    if (unitGrid == null) return null;
    int index = getChunkIndex(x, y);
    return unitGrid[index];
  }

  /**
   * 彻底清理并准备接收新数据的环境
   *
   * @param newW 新地图的宽
   * @param newH 新地图的高
   */
  public static void reBuildAll(int newW, int newH) {
    
    if (units != null) {
      units.each(
        unit -> {
          unit.reset();
        }
      );
      units.clear();
    }
    if (moveunits != null) {
      moveunits.clear();
    }
    unitGrid = null;
    
    Teams.init();

    world = new World(newW, newH, false);
    world.floors = new Ar<>(newW * newH);
    world.envblocks = new Ar<>(newW * newH);

    // 重置空间网格 (Spatial Grid)
    gridW = Mathf.ceil((float) newW / CHUNK_SIZE);
    gridH = Mathf.ceil((float) newH / CHUNK_SIZE);

    int totalChunks = gridW * gridH;
    unitGrid = new Ar[totalChunks];
    for (int i = 0; i < totalChunks; i++) {
      unitGrid[i] = new Ar<>(16);
    }
    Events.fire(EventType.events.Mapinit);
    RouteData.init();
  }
}
