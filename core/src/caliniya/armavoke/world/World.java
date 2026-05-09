package caliniya.armavoke.world;

import arc.math.Mathf;
import arc.func.Intc2;
import caliniya.armavoke.content.Blocks; // 确保导入
import caliniya.armavoke.content.ENVBlocks;
import caliniya.armavoke.content.Floors;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.base.type.TeamTypes; // 导入阵营
import caliniya.armavoke.base.game.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.game.data.*;

public class World {
  public boolean space;
  public int W, H;

  public boolean test = true;

  public WorldChunk[] chunks;

  public int chunksW, chunksH;

  public World(int W, int H, boolean space) {
    this.W = W;
    this.H = H;
    this.space = space;

    this.chunksW = (W + WorldChunk.MASK) >> WorldChunk.SHIFT;
    this.chunksH = (H + WorldChunk.MASK) >> WorldChunk.SHIFT;
    this.chunks = new WorldChunk[chunksW * chunksH];
  }

  public void init() {
    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = null;
    }

    if (test) {
      // 填充基础地板
      if (!space) {
        for (int y = 0; y < H; y++) {
          for (int x = 0; x < W; x++) {
            setFloor(x, y, Floors.TestFloor);
          }
        }
      }

      // 生成随机障碍物
      int padding = 5;
      for (int y = padding; y < H - padding; y++) {
        for (int x = padding; x < W - padding; x++) {
          if (Math.random() < 0.003) {
            for (int oy = -1; oy <= 1; oy++) {
              for (int ox = -1; ox <= 1; ox++) {
                int px = x + ox;
                int py = y + oy;
                if (isValidCoord(px, py)) {
                  setENVBlock(px, py, ENVBlocks.a);
                }
              }
            }
          }
        }
      }

      // 生成随机测试建筑
      int buildingCount = 3;
      for (int i = 0; i < buildingCount; i++) {
        int bx = padding + (int) (Math.random() * (W - padding * 2));
        int by = padding + (int) (Math.random() * (H - padding * 2));
        if (isSolid(bx, by)) {
          i--;
          continue;
        }
        setBuilding(bx, by, Blocks.TestBlock);
      }
    }

    // --- 新增：在地图中心生成敌方测试炮塔 ---
    int centerX = W / 2;
    int centerY = H / 2;
    
    Building enemyTurret = setBuilding(centerX, centerY, Blocks.testTurret);

      enemyTurret.team = TeamTypes.Mutex;
  }

  // --- 区块管理辅助方法 ---

  private int getChunkIndex(int cx, int cy) {
    return cy * chunksW + cx;
  }

  private WorldChunk getChunk(int x, int y) {
    int cx = x >> WorldChunk.SHIFT;
    int cy = y >> WorldChunk.SHIFT;
    int idx = cy * chunksW + cx;
    if (idx < 0 || idx >= chunks.length) return null;
    return chunks[idx];
  }

  private WorldChunk getOrCreateChunk(int x, int y) {
    int cx = x >> WorldChunk.SHIFT;
    int cy = y >> WorldChunk.SHIFT;
    int idx = cy * chunksW + cx;
    if (idx < 0 || idx >= chunks.length) return null;
    if (chunks[idx] == null) chunks[idx] = new WorldChunk();
    return chunks[idx];
  }

  // --- 建筑逻辑 ---

  public Building getBuilding(int x, int y) {
    if (!isValidCoord(x, y)) return null;
    WorldChunk chunk = getChunk(x, y);
    if (chunk == null) return null;
    return chunk.getBuilding(x & WorldChunk.MASK, y & WorldChunk.MASK);
  }

  public boolean hasBuilding(int x, int y) {
    return getBuilding(x, y) != null;
  }

  public Building setBuilding(int x, int y, Block block) {
    if (!isValidCoord(x, y) || block == null) return null;

    Building newBuild = block.create(x, y);
    WorldData.buildings.add(newBuild);

    newBuild.getOccupiedCoords(
        (tx, ty) -> {
          if (isValidCoord(tx, ty)) {
            Building existing = getBuilding(tx, ty);
            if (existing != null && existing != newBuild) {
              removeBuilding(existing.tx, existing.ty);
            }
            WorldChunk chunk = getOrCreateChunk(tx, ty);
            chunk.setBuilding(tx & WorldChunk.MASK, ty & WorldChunk.MASK, newBuild);
          }
        });

    return newBuild;
  }

  public void removeBuilding(int x, int y) {
    Building build = getBuilding(x, y);
    if (build == null) return;

    WorldData.buildings.remove(build);
    build.getOccupiedCoords(
        (tx, ty) -> {
          if (isValidCoord(tx, ty)) {
            WorldChunk chunk = getChunk(tx, ty);
            if (chunk != null
                && chunk.getBuilding(tx & WorldChunk.MASK, ty & WorldChunk.MASK) == build) {
              chunk.setBuilding(tx & WorldChunk.MASK, ty & WorldChunk.MASK, null);
            }
          }
        });

    build.remove();
  }

  public boolean isSolid(int x, int y) {
    if (!isValidCoord(x, y)) return true;
    WorldChunk chunk = getChunk(x, y);
    if (chunk != null && chunk.getENVBlock(x & WorldChunk.MASK, y & WorldChunk.MASK) != 0) {
      return true;
    }
    Building b = getBuilding(x, y);
    if (b != null && b.block.solid) {
      return true;
    }
    return false;
  }

  // --- 环境方块 & 地板 (保持不变) ---

  public void setENVBlock(int x, int y, ENVBlock block) {
    if (!isValidCoord(x, y)) return;
    short id = (block == null) ? 0 : block.id;
    if (id == 0) {
      WorldChunk chunk = getChunk(x, y);
      if (chunk == null) return;
      chunk.setENVBlock(x & WorldChunk.MASK, y & WorldChunk.MASK, (short) 0);
    } else {
      WorldChunk chunk = getOrCreateChunk(x, y);
      chunk.setENVBlock(x & WorldChunk.MASK, y & WorldChunk.MASK, id);
    }
  }

  public short getENVBlockId(int x, int y) {
    if (!isValidCoord(x, y)) return 0;
    WorldChunk chunk = getChunk(x, y);
    if (chunk == null) return 0;
    return chunk.getENVBlock(x & WorldChunk.MASK, y & WorldChunk.MASK);
  }

  public ENVBlock getENVBlock(int x, int y) {
    short id = getENVBlockId(x, y);
    if (id == 0) return null;
    return Contents.getByID(CType.ENVBlock, id);
  }

  public void setFloor(int x, int y, Floor floor) {
    if (!isValidCoord(x, y)) return;
    short id = (floor == null) ? 0 : floor.id;
    if (id == 0) {
      WorldChunk chunk = getChunk(x, y);
      if (chunk == null) return;
      chunk.setFloor(x & WorldChunk.MASK, y & WorldChunk.MASK, (short) 0);
    } else {
      WorldChunk chunk = getOrCreateChunk(x, y);
      chunk.setFloor(x & WorldChunk.MASK, y & WorldChunk.MASK, id);
    }
  }

  public short getFloorId(int x, int y) {
    if (!isValidCoord(x, y)) return 0;
    WorldChunk chunk = getChunk(x, y);
    if (chunk == null) return 0;
    return chunk.getFloor(x & WorldChunk.MASK, y & WorldChunk.MASK);
  }

  public Floor getFloor(int x, int y) {
    short id = getFloorId(x, y);
    if (id == 0) return null;
    return Contents.getByID(CType.Floor, id);
  }

  public boolean isValidCoord(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }

  // --- 索引相关方法 ---

  public void setENVBlock(int index, ENVBlock block) {
    setENVBlock(index % W, index / W, block);
  }

  public short getENVBlockId(int index) {
    return getENVBlockId(index % W, index / W);
  }

  public ENVBlock getENVBlock(int index) {
    return getENVBlock(index % W, index / W);
  }

  public void setFloor(int index, Floor floor) {
    setFloor(index % W, index / W, floor);
  }

  public short getFloorId(int index) {
    return getFloorId(index % W, index / W);
  }

  public Floor getFloor(int index) {
    return getFloor(index % W, index / W);
  }

  public boolean isSolid(int index) {
    return isSolid(index % W, index / W);
  }

  public int coordToIndex(int x, int y) {
    return y * W + x;
  }
}
