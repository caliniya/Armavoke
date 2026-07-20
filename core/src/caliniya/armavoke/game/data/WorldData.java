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
  public volatile static Ar<Building> buildings;

  // 有移动目标的单位
  public static Ar<Unit> moveunits;
  // 子弹
  public static Ar<Bullet> bullets;

  // --- 空间划分相关 ---
  // 每个区块包含的瓦片数量 (32x32个地块)
  public static final int CHUNK_SIZE = 32;
  // 单个地块的像素大小
  public static final int TILE_SIZE = 32;
  // 单个区块的像素大小 (32 * 32 = 1024)
  public static final int CHUNK_PIXEL_SIZE = CHUNK_SIZE * TILE_SIZE;

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

    Teams.init();
    RouteData.init();

    // 初始化四叉树覆盖范围（世界像素尺寸）
    float worldPixelW = world.W * TILE_SIZE;
    float worldPixelH = world.H * TILE_SIZE;
    Teams.initAllTrees(worldPixelW, worldPixelH);
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


}
