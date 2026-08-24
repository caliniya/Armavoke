package caliniya.armavoke.game.data;

import arc.*;
import arc.util.Log;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.content.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.base.game.EntityAr;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.world.*;
import arc.math.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;

public class WorldData {
  public static World world;

  // ========== 全局实体容器 ==========
  public static EntityAr<Unit> units;
  public static EntityAr<Building> buildings;
  public static EntityAr<Unit> moveunits;
  public static EntityAr<Bullet> bullets;

  // --- 空间划分相关 ---
  public static final int CHUNK_SIZE = 32;
  public static final int TILE_SIZE = 32;
  public static final int CHUNK_PIXEL_SIZE = CHUNK_SIZE * TILE_SIZE;

  private WorldData() {}

  @SuppressWarnings("unchecked")
  public static void initWorld(int w, int h, boolean space) {
    Game.team = TeamTypes.Evoke;

    units = new EntityAr<>(unit -> unit.id);
    buildings = new EntityAr<>(building -> building.id);
    moveunits = new EntityAr<>(unit -> unit.id);
    bullets = new EntityAr<>(bullet -> bullet.id);

    world = new World(w, h, space);
    world.init();

    Teams.init();

    RouteData.init();

    // 初始化四叉树覆盖范围
    float worldPixelW = world.W * TILE_SIZE;
    float worldPixelH = world.H * TILE_SIZE;
    initAllTrees(worldPixelW, worldPixelH);
  }

  public static void initAllTrees(float worldPixelW, float worldPixelH) {
    if (units != null) units.resize(0, 0, worldPixelW, worldPixelH);
    if (buildings != null) buildings.resize(0, 0, worldPixelW, worldPixelH);
    if (moveunits != null) moveunits.resize(0, 0, worldPixelW, worldPixelH);
    if (bullets != null) bullets.resize(0, 0, worldPixelW, worldPixelH);
    // 同步子弹处理系统的内部子弹树（力场拦截等依赖它的 intersect）
    if (Systems.BP != null) Systems.BP.resizeTree(worldPixelW, worldPixelH);
  }

  public static void clear() {
    if (units != null) units.clear(unit -> unit.reset());
    if (buildings != null) buildings.clear(building -> building.remove());
    if (moveunits != null) moveunits.clear(unit -> unit.reset());
    if (bullets != null) bullets.clear();
  }
}
