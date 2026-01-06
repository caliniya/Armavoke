package caliniya.armavoke.world;

import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.content.ENVBlocks;
import caliniya.armavoke.content.Floors;


//表示世界环境的静态数据
public class World {
  public boolean space;
  public int W, H;
  public boolean test = true;

  public Ar<Floor> floors = new Ar<>(10000);
  public Ar<ENVBlock> envblocks = new Ar<>(10000);

  public World() {
    this(100, 100, true);
  }

  public World(int W, int H, boolean space) {
    this.W = W;
    this.H = H;
    this.space = space;
    this.test = true;
  }

  public void init() {
    floors.clear();
    envblocks.clear();
    floors.ensureCapacity(W * H);
    envblocks.ensureCapacity(W * H);

    for (int i = 0; i < W * H; i++) {
      if (space) {
        floors.add((Floor) null); // 太空无地板
      } else {
        floors.add(Floors.TestFloor); // 地表有地板
      }
      envblocks.add((ENVBlock)null);
    }
    
    if (test) {
      int padding = 5;
      for (int y = padding; y < H - padding; y++) {
        for (int x = padding; x < W - padding; x++) {
          // 随机生成一簇障碍
          if (Math.random() < 0.003) {
            for (int oy = -1; oy <= 1; oy++) {
              for (int ox = -1; ox <= 1; ox++) {
                int px = x + ox;
                int py = y + oy;
                if (isValidCoord(px, py)) {
                  int idx = coordToIndex(px, py);
                  envblocks.set(idx, ENVBlocks.a);
                }
              }
            }
          }
        }
      }
    }
  }

  public int coordToIndex(int x, int y) {
    return y * W + x;
  }

  public boolean isValidCoord(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }

  public boolean isSolid(int x, int y) {
    if (!isValidCoord(x, y)) return true; // 边界也是墙
    int index = coordToIndex(x, y);
    if (index >= envblocks.size) return false;
    return envblocks.get(index) != null;
  }

  public boolean isSolid(int index) {
    return isSolid(index % W, index / W);
  }
}
