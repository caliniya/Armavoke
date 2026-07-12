package caliniya.armavoke.game.data;

import arc.*;
import arc.util.Log;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.content.Blocks;
import caliniya.armavoke.core.*;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.world.*;
import arc.math.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;

public class WorldData {
  public static World world;

  // 全局单位列表 (用于逻辑更新)
  public static Ar<Unit> units;
  // 全局建筑列表 (用于逻辑更新)
  public static Ar<Building> buildings;

  // 有移动目标的单位
  public static Ar<Unit> moveunits;
  // 子弹
  public static Ar<Bullet> bullets;

  // --- 空间划分网格相关 ---
  // 每个区块包含的瓦片数量 (32x32个地块)
  public static final int CHUNK_SIZE = 32;
  // 单个地块的像素大小
  public static final int TILE_SIZE = 32;
  // 单个区块的像素大小 (32 * 32 = 1024)
  public static final int CHUNK_PIXEL_SIZE = CHUNK_SIZE * TILE_SIZE;

  // 网格的宽和高 (以区块为单位)
  public static int gridW, gridH;

  private WorldData() {}

  @SuppressWarnings("unchecked")
  public static void initWorld(int w, int h, boolean space) {

    Game.team = TeamTypes.Evoke;
    units = new Ar<>(100);
    buildings = new Ar<>(100);
    // 有移动目标的单位
    moveunits = new Ar<>(5);
    bullets = new Ar<>(false, 1000);
    world = new World(w, h, space);
    world.init();

    // 1. 初始化网格尺寸
    // 即使地图大小不能整除32，也要向上取整多算一个格子，防止越界
    gridW = Mathf.ceil((float) world.W / CHUNK_SIZE);
    gridH = Mathf.ceil((float) world.H / CHUNK_SIZE);

    Teams.init();
    RouteData.init();
  }

  public static void clear() {
    if (units != null) {
      units.each(
          unit -> {
            unit.reset();
          });
      units.clear();
    }

    // 清理建筑
    if (buildings != null) {
      buildings.each(b -> b.remove()); // 使用 remove 归还对象池,kill会有回调
      buildings.clear();
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
}
