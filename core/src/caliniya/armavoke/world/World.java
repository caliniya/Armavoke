package caliniya.armavoke.world;

import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.content.ENVBlocks;
import caliniya.armavoke.content.Floors;
import caliniya.armavoke.game.Unit;

// 这个类表示一个世界
// 数据不在这里，数据在WorrldData里
// 在初始化世界数据时同步创建一个此世界对象
// 这个类表示的是世界静态内容
public class World {

  public boolean space; // 表示这是太空还是地表(当然离实现太空还早)
  public int W;
  public int H;
  public int index;

  public boolean test = true; // 还早着

  public Ar<Floor> floors = new Ar<Floor>(10000);
  public Ar<ENVBlock> envblocks = new Ar<ENVBlock>(10000);

  public World() {
    test = true;
    W = 100;
    H = 100;
  }

  public World(int W, int H, boolean space) {
    this.W = W;
    this.H = H;
    this.space = space;
    this.index = W * H;
    test = false;
  }

  public void init() {
    if (test) {
      floors.ensureCapacity(W * H);
      envblocks.ensureCapacity(W * H);

      // 1. 初始化空地图
      for (int i = 0; i < W * H; i++) {
        floors.add(Floors.TestFloor);
        envblocks.add((ENVBlock) null);
      }

      int padding = 5;

      // 2. 随机生成 3x3 的方块簇
      // 概率可以调高一点，比如 0.01 (1%)，因为我们不再是逐格生成
      for (int y = padding; y < H - padding; y++) {
        for (int x = padding; x < W - padding; x++) {

          // 如果随机命中，则以此 (x,y) 为中心生成一个 3x3 方块
          if (Math.random() < 0.003) {

            // 遍历 (x-1, y-1) 到 (x+1, y+1) 的区域
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
              for (int offsetX = -1; offsetX <= 1; offsetX++) {
                int placeX = x + offsetX;
                int placeY = y + offsetY;

                // 确保生成位置在地图内
                if (isValidCoord(placeX, placeY)) {
                  int index = coordToIndex(placeX, placeY);
                  envblocks.set(index, ENVBlocks.a);
                }
              }
            }
          }
        }
      }

      // 3. 生成固定的测试墙体 (例如一条直线)
      // 这个循环应该在外面，只执行一次
      for (int y = 0; y < 15; y++) {
        int x = 10;
        if (isValidCoord(x, y)) {
          int index = coordToIndex(x, y);
          envblocks.set(index, ENVBlocks.a);
        }
      }
    }

    int testX = 5;
    int testY = 0;
    int testIndex = coordToIndex(testX, testY);
    // 强制清空周围，确保它是孤立的
    for (int i = -2; i <= 2; i++)
      for (int j = 0; j <= 2; j++)
        if (isValidCoord(testX + i, testY + j))
          envblocks.set(coordToIndex(testX + i, testY + j), null);

    // 放置目标块
    envblocks.set(testIndex, ENVBlocks.a);
    Log.info("TEST BLOCK PLACED AT: x=@, y=@, index=@", testX, testY, testIndex);
    Log.info("Stack trace:", new RuntimeException("Trace World Init"));
  }

  public int indexToX(int ind) {
    return ind % W;
  }

  public int indexToY(int ind) {
    return ind / W;
  }

  // 根据坐标获取索引
  public int coordToIndex(int x, int y) {
    return y * W + x;
  }

  // 检查坐标是否有效
  public boolean isValidCoord(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }

  /**
   * 【核心方法】检查一个网格坐标是否是障碍物 (无法通行)
   *
   * @param x 网格 x 坐标
   * @param y 网格 y 坐标
   * @return 如果是障碍物则返回 true
   */
  public boolean isSolid(int x, int y) {
    // 1. 检查坐标是否在地图范围内
    if (!isValidCoord(x, y)) {
      return true; // 地图外的区域视为障碍物
    }

    // 2. 检查该位置是否有环境块
    int index = coordToIndex(x, y);

    // 安全检查，防止数组未完全初始化
    if (index >= envblocks.size) {
      return false; // 如果没有环境块数据，则默认可以通过
    }
    ENVBlock block = envblocks.get(index);
    // 如果这个位置有方块 (不是 null)，并且这个方块是无法通行的
    // （未来可以给 ENVBlock 加一个 isSolid 属性）
    // 目前我们假设只要有环境块就不能通过
    return block != null;
  }

  public boolean isSolid(int index) {
    return isSolid(indexToX(index), indexToY(index));
  }
}
