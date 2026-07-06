package caliniya.armavoke.world;

import arc.Core;
import arc.func.Intc2;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.core.*;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.*;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.type.type.ItemType;

public class Block extends ContentType {

  // --- 基础属性 ---
  public float psize; // 大小，像素级
  public int size = 2; // 大小，单位格
  public boolean buildable = true; // 可以造
  public boolean solid = true; // 可以阻挡通行
  public float health = 100; // 顾名思义
  public int capacity = 100; // 物品容量，为0就是不能存
  public ItemType[] allowItem = Contents.items; // 能存的,默认啥都能存一百

  public TextureRegion region; // 主贴图

  // --- 形状定义 ---
  // 相对于锚点(0,0)的偏移量数组：[dx1, dy1, dx2, dy2, ...]
  public int[] shapeOffsets = null;

  public Block(String name) {
    super(name, CType.Block);
  }

  public Building create(int tx, int ty) {
    psize = size * WorldData.TILE_SIZE;
    return Building.create(this, tx, ty);
  }

  public Building create(int tx, int ty, TeamTypes team) {
    psize = size * WorldData.TILE_SIZE;
    return Building.create(this, tx, ty, team);
  }

  public void load() {
    region = Core.atlas.find(name);
  }

  public void update(Building building, float dt) {}

  public void draw(Building b) {
    float rotation = b.angle * 90f;
    Draw.rect(region, b.x + psize / 2, b.y + psize / 2, rotation);
  }

  public void write(Building b, Writes w) {}

  public void read(Building b, Reads r) {}

  // --- 目标查找 ---

  /** 查找目标实体。默认空实现，子类（如炮塔）可覆写。 */
  public Entity findTarget(Building b) {
    return null;
  }

  // --- 物品相关 ---

  public void allowAllItem(ItemType... types) {
    allowItem = types;
  }

  /**
   * 辅助方法：获取旋转后的形状偏移量
   * 这用于确定建筑在当前角度下实际占据了哪些格子
   *
   * @param angle 建筑当前角度 (0-3)
   * @param baseOffsets 原始形状偏移 (通常是 block.shapeOffsets)
   * @return 旋转后的新偏移量数组
   */
  public static int[] getRotatedOffsets(int angle, int[] baseOffsets) {
    if (baseOffsets == null) return new int[] {0, 0};

    int[] rotated = baseOffsets.clone();

    for (int i = 0; i < rotated.length; i += 2) {
      int x = baseOffsets[i];
      int y = baseOffsets[i + 1];

      switch (angle) {
        case 1:
          rotated[i] = y;
          rotated[i + 1] = -x;
          break;
        case 2:
          rotated[i] = -x;
          rotated[i + 1] = -y;
          break;
        case 3:
          rotated[i] = -y;
          rotated[i + 1] = x;
          break;
        default:
          rotated[i] = x;
          rotated[i + 1] = y;
          break;
      }
    }
    return rotated;
  }
}
