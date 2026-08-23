package caliniya.armavoke.world;

import caliniya.armavoke.base.game.WorldChunk;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.runtime.EcsRuntime;
import caliniya.armavoke.game.Contents;
import caliniya.armavoke.game.data.RouteData;
import caliniya.armavoke.type.Building;

/** Chunked terrain plus references to authoritative ECS building entities. */
public class World {
  public final int W;
  public final int H;
  public final boolean space;
  public final int chunksW;
  public final int chunksH;
  public final WorldChunk[] chunks;

  public World(int width, int height, boolean space) {
    this.W = Math.max(1, width);
    this.H = Math.max(1, height);
    this.space = space;
    this.chunksW = (W + WorldChunk.MASK) >> WorldChunk.SHIFT;
    this.chunksH = (H + WorldChunk.MASK) >> WorldChunk.SHIFT;
    this.chunks = new WorldChunk[chunksW * chunksH];
  }

  public void init() {}

  public boolean isValidCoord(int x, int y) { return x >= 0 && y >= 0 && x < W && y < H; }

  private WorldChunk chunk(int x, int y, boolean create) {
    if (!isValidCoord(x, y)) return null;
    int index = (y >> WorldChunk.SHIFT) * chunksW + (x >> WorldChunk.SHIFT);
    WorldChunk value = chunks[index];
    if (value == null && create) chunks[index] = value = new WorldChunk();
    return value;
  }

  public Building getBuilding(int x, int y) {
    WorldChunk value = chunk(x, y, false);
    return value == null ? null : value.getBuilding(x & WorldChunk.MASK, y & WorldChunk.MASK);
  }

  public boolean hasBuilding(int x, int y) { return getBuilding(x, y) != null; }

  public Building setBuilding(int x, int y, Block block, TeamTypes team) {
    if (block == null || !isValidCoord(x, y)) return null;
    Building building = block.create(x, y, team);
    if (!setBuilding(building)) {
      EcsRuntime.remove(building);
      return null;
    }
    return building;
  }

  public boolean setBuilding(Building building) {
    if (building == null || building.block() == null || !isValidCoord(building.tx(), building.ty())) return false;
    final boolean[] valid = {true};
    building.getOccupiedCoords((x, y) -> {
      if (!isValidCoord(x, y) || getBuilding(x, y) != null) valid[0] = false;
    });
    if (!valid[0]) return false;
    building.getOccupiedCoords((x, y) -> {
      chunk(x, y, true).setBuilding(x & WorldChunk.MASK, y & WorldChunk.MASK, building);
      RouteData.updateBlock(x, y, building.block().solid);
    });
    return true;
  }

  public void removeBuilding(int x, int y) {
    Building building = getBuilding(x, y);
    if (building == null) return;
    removeBuilding(building);
    EcsRuntime.remove(building);
  }

  public void removeBuilding(Building building) {
    if (building == null) return;
    building.getOccupiedCoords((x, y) -> {
      WorldChunk value = chunk(x, y, false);
      if (value != null && value.getBuilding(x & WorldChunk.MASK, y & WorldChunk.MASK) == building) {
        value.setBuilding(x & WorldChunk.MASK, y & WorldChunk.MASK, null);
        RouteData.updateBlock(x, y, false);
      }
    });
  }

  public boolean isSolid(int index) {
    return index < 0 || index >= W * H || isSolid(index % W, index / W);
  }

  public boolean isSolid(int x, int y) {
    if (!isValidCoord(x, y)) return true;
    ENVBlock env = getENVBlock(x, y);
    if (env != null && env.solid) return true;
    Building building = getBuilding(x, y);
    return building != null && building.block() != null && building.block().solid;
  }

  public void setENVBlock(int x, int y, ENVBlock block) {
    if (!isValidCoord(x, y)) return;
    chunk(x, y, true).setENVBlock(x & WorldChunk.MASK, y & WorldChunk.MASK, block == null ? 0 : block.id);
  }

  public ENVBlock getENVBlock(int x, int y) {
    WorldChunk value = chunk(x, y, false);
    int id = value == null ? 0 : value.getENVBlock(x & WorldChunk.MASK, y & WorldChunk.MASK);
    return id <= 0 ? null : Contents.getByID(CType.ENVBlock, id);
  }

  public void setFloor(int x, int y, Floor floor) {
    if (!isValidCoord(x, y)) return;
    chunk(x, y, true).setFloor(x & WorldChunk.MASK, y & WorldChunk.MASK, floor == null ? 0 : floor.id);
  }

  public Floor getFloor(int x, int y) {
    WorldChunk value = chunk(x, y, false);
    int id = value == null ? 0 : value.getFloor(x & WorldChunk.MASK, y & WorldChunk.MASK);
    return id <= 0 ? null : Contents.getByID(CType.Floor, id);
  }
}
