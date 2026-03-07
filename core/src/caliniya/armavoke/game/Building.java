package caliniya.armavoke.game;

import caliniya.armavoke.world.*;
import arc.util.pooling.Pools;
import arc.util.pooling.Pool.Poolable;

public class Building implements Poolable {

  public Block block;
  // 我们以左下角为坐标点
  public int x, y;

  protected Building() {}

  /**
   * 从对象池中获取一个 Building 实例。
   *
   * @return 初始化后的 Building 实例
   */
  public static Building create(Block block, int x, int y) {

    Building building = Pools.obtain(Building.class, Building::new);
    building.block = block;
    building.x = x;
    building.y = y;
    return building;
  }

  public void remove() {
    Pools.free(this);
  }

  @Override
  public void reset() {
    block = null;
    x = 0;
    y = 0;
  }
}
