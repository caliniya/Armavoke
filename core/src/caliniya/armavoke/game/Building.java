package caliniya.armavoke.game;

import arc.func.Intc2;
import arc.util.pooling.Pools;
import arc.util.pooling.Pool.Poolable;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.world.Block;

public class Building implements Poolable {

  // --- 锚点坐标 (左下角起始点) ---
  // 0:上, 1:右, 2:下, 3:左
  public int tx, ty, angle;
  public float x, y;

  // --- 运行时状态数据 ---
  public float maxHealth;
  public float health;
  public ItemModule item;

  // --- 形状数据 (独立副本，已旋转) ---
  // 这里的 shapeOffsets 是经过 angle 变换后的世界相对坐标
  public int[] shapeOffsets;

  public Block block;

  protected Building() {}

  /** 初始化建筑实体 */
  public void init(Block block, int tx, int ty, int angle) {
    this.block = block;
    this.tx = tx;
    this.ty = ty;
    this.angle = angle % 4; // 确保角度在 0-3 之间

    // 计算像素坐标
    this.x = tx * WorldData.TILE_SIZE;
    this.y = ty * WorldData.TILE_SIZE;

    this.maxHealth = block.health;
    this.health = block.health;
    
    this.item = new ItemModule(block.capacity);
    item.setFilter(block.allowItem);

    // 计算旋转后的形状数据
    if (block.shapeOffsets != null) {
      this.shapeOffsets = Block.getRotatedOffsets(this.angle, block.shapeOffsets);
    } else {
      // 如果没有异形定义，确保为 null，使用 size 逻辑
      this.shapeOffsets = null;
    }
  }

  // 重载方法，默认角度为0
  public void init(Block block, int tx, int ty) {
    init(block, tx, ty, 0);
  }

  public void update() {
    // 如果有动画或移动，在这里更新像素坐标
    x = tx * WorldData.TILE_SIZE;
    y = ty * WorldData.TILE_SIZE;
  }

  /** 计算该建筑占据的所有世界坐标 */
  public void getOccupiedCoords(Intc2 consumer) {
    // 情况 1: 异形建筑 (有自定义形状数据)
    if (shapeOffsets != null) {
      for (int i = 0; i < shapeOffsets.length; i += 2) {
        consumer.get(tx + shapeOffsets[i], ty + shapeOffsets[i + 1]);
      }
    }
    // 情况 2: 标准矩形建筑 (无自定义形状数据，依据 size 判定)
    else if (block != null) {
      int s = (int) block.size;
      // 遍历 size x size 的区域
      // 例如 size=2 时，遍历 (0,0), (0,1), (1,0), (1,1)
      for (int dx = 0; dx < s; dx++) {
        for (int dy = 0; dy < s; dy++) {
          consumer.get(tx + dx, ty + dy);
        }
      }
    }
  }

  /** 交互逻辑：判断是否占据指定坐标，参数为格坐标 */
  public boolean occupies(int worldX, int worldY) {
    // 情况 1: 异形判断
    if (shapeOffsets != null) {
      for (int i = 0; i < shapeOffsets.length; i += 2) {
        // 注意：这里使用 tx (瓦片坐标) 而不是 x (像素坐标)
        if (tx + shapeOffsets[i] == worldX && ty + shapeOffsets[i + 1] == worldY) {
          return true;
        }
      }
      return false;
    }

    // 情况 2: 矩形判断
    if (block != null) {
      int s = (int) block.size;
      // 检查是否在 [tx, tx+s) 和 [ty, ty+s) 范围内
      return worldX >= tx && worldX < tx + s && worldY >= ty && worldY < ty + s;
    }

    return false;
  }

  public void draw() {
    block.draw(this);
  }

  // --- 对象池管理 ---

  public static Building create(Block block, int tx, int ty, int angle) {
    Building building = Pools.obtain(Building.class, Building::new);
    building.init(block, tx, ty, angle);
    return building;
  }

  public static Building create(Block block, int tx, int ty) {
    return create(block, tx, ty, 0);
  }

  public void remove() {
    Pools.free(this);
  }

  @Override
  public void reset() {
    block = null;
    shapeOffsets = null;
    tx = 0;
    ty = 0;
    x = 0;
    y = 0;
    angle = 0;
    health = 0;
    maxHealth = 0;
  }
}
