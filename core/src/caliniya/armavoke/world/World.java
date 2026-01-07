package caliniya.armavoke.world;

import arc.math.Mathf;
import caliniya.armavoke.content.ENVBlocks;
import caliniya.armavoke.content.Floors;
import caliniya.armavoke.core.ContentVar;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.base.game.*;

public class World {
  public boolean space;
  public int W, H;
  
  // 保留 test 变量
  public boolean test = true;

  public WorldChunk[] chunks;

  public int chunksW, chunksH;

  public World(int W, int H, boolean space) {
    this.W = W;
    this.H = H;
    this.space = space;

    // 计算区块数量
    // 32 = 1 << 5
    // 使用位运算向上取整: (W + 31) >> 5
    this.chunksW = (W + WorldChunk.MASK) >> WorldChunk.SHIFT;
    this.chunksH = (H + WorldChunk.MASK) >> WorldChunk.SHIFT;
        // 分配连续的一维数组
    this.chunks = new WorldChunk[chunksW * chunksH];
  }

  public void init() {
    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = null;
    }
    
    if (test) {
      // 填充基础地板
      // 注意：如果是太空地图(space=true)，默认就是空的(null)，不需要填充
      // 只有地表地图才需要铺满地板
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
          // 随机生成一簇障碍 (概率 0.3%)
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
    }
  }

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
    return ContentVar.getByID(CType.ENVBlock, id);
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
    return ContentVar.getByID(CType.Floor, id);
  }

  public boolean isSolid(int x, int y) {
    if (!isValidCoord(x, y)) return true;
    int cx = x >> WorldChunk.SHIFT;
    int cy = y >> WorldChunk.SHIFT;
    int idx = cy * chunksW + cx;
    if (chunks[idx] == null) return false;
    short id = chunks[idx].getENVBlock(x & WorldChunk.MASK, y & WorldChunk.MASK);
    return id != 0;
  }

  public boolean isValidCoord(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }
  
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