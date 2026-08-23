package caliniya.armavoke.game.data;

import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.runtime.EcsRuntime;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.game.Teams;
import caliniya.armavoke.world.World;

/** World-level data only. Gameplay entities live exclusively in the ECS world. */
public final class WorldData {
  public static World world;
  public static final int CHUNK_SIZE = 32;
  public static final int TILE_SIZE = 32;
  public static final int tilesize = TILE_SIZE;
  public static final int CHUNK_PIXEL_SIZE = CHUNK_SIZE * TILE_SIZE;

  private WorldData() {}

  public static void initWorld(int width, int height, boolean space) {
    if (EcsRuntime.world() != null) EcsRuntime.clear();
    Game.team = TeamTypes.Evoke;
    world = new World(width, height, space);
    world.init();
    Teams.init();
    RouteData.init();
  }

  /** Kept as a no-op compatibility entry point; ECS queries need no spatial mirror tree. */
  public static void initAllTrees(float worldPixelWidth, float worldPixelHeight) {}

  public static void clear() {
    EcsRuntime.clear();
  }
}
