package caliniya.armavoke.world;

import arc.Core;
import arc.func.Intc2;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.data.WorldData;

public class Block extends ContentType {

  // --- 基础属性 ---
  public float psize;
  public int size;
  public boolean buildable = true;
  public boolean solid = true;
  public float health = 100;

  public TextureRegion region;

  // --- 形状定义 ---
  // 相对于锚点(0,0)的偏移量数组：[dx1, dy1, dx2, dy2, ...]
  public int[] shapeOffsets = null;

  public Block(String name) {
    super(name, CType.Block);
    load();
  }

  public Building create(int tx, int ty) {
    psize = size * WorldData.TILE_SIZE;
    return Building.create(this, tx, ty);
  }

  public void load() {
    region = Core.atlas.find(name);
  }

  /** 绘制方法 实现核心逻辑： 1. 以建筑中心为旋转中心。 2. 根据角度顺时针旋转。 */
  public void draw(Building b) {
    float rotation = b.angle * 90f;
    Draw.rect(region, b.x + psize / 2, b.y + psize / 2, rotation);
  }

  /**
   * 辅助方法：获取旋转后的形状偏移量 这用于确定建筑在当前角度下实际占据了哪些格子
   *
   * @param angle 建筑当前角度 (0-3)
   * @param baseOffsets 原始形状偏移 (通常是 block.shapeOffsets)
   * @return 旋转后的新偏移量数组
   */
  public static int[] getRotatedOffsets(int angle, int[] baseOffsets) {
    if (baseOffsets == null) return new int[] {0, 0};

    int[] rotated = baseOffsets.clone();

    // 根据 angle 顺时针旋转坐标
    // 0: (x, y) -> (x, y)
    // 1: (x, y) -> (y, -x)  (顺时针90度)
    // 2: (x, y) -> (-x, -y) (180度)
    // 3: (x, y) -> (-y, x)  (顺时针270度/逆时针90度)

    for (int i = 0; i < rotated.length; i += 2) {
      int x = baseOffsets[i];
      int y = baseOffsets[i + 1];

      switch (angle) {
        case 1: // 右转90度
          rotated[i] = y;
          rotated[i + 1] = -x;
          break;
        case 2: // 右转180度
          rotated[i] = -x;
          rotated[i + 1] = -y;
          break;
        case 3: // 右转270度
          rotated[i] = -y;
          rotated[i + 1] = x;
          break;
        default: // 0度
          rotated[i] = x;
          rotated[i + 1] = y;
          break;
      }
    }
    return rotated;
  }
}
