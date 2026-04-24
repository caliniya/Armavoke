package caliniya.armavoke.game;

import arc.func.Intc2;
import arc.util.io.*;
import arc.util.pooling.Pools;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.type.module.*;
import caliniya.armavoke.world.*;
import caliniya.armavoke.base.game.*;

public class Building extends Entity {

  // --- 锚点坐标 (左下角起始点) ---
  // 0:上, 1:右, 2:下, 3:左
  public int tx, ty, angle;

  // --- 形状数据 (独立副本，已旋转) ---
  public int[] shapeOffsets;

  public Block block;

  protected Building() {}

  /** 初始化建筑实体 */
  public void init() {
    this.block = block;
    this.tx = tx;
    this.ty = ty;
    this.angle = angle % 4;

    // 初始化坐标
    this.x = tx * WorldData.TILE_SIZE;
    this.y = ty * WorldData.TILE_SIZE;

    // 初始化血量
    this.maxHealth = block.health;
    this.health = block.health;

    // 初始化物品
    this.item = new ItemModule(block.capacity);
    this.item.setFilter(block.allowItem);
    
    this.id = Entities.assignID();

    // 计算旋转后的形状数据
    if (block.shapeOffsets != null) {
      this.shapeOffsets = Block.getRotatedOffsets(this.angle, block.shapeOffsets);
    } else {
      this.shapeOffsets = null;
    }
  }

  @Override
  public void update(float dt) {
    block.uptate(this,dt);
  }

  /** 计算该建筑占据的所有世界坐标 */
  //参数是对每个坐标进行的操作
  //瓦片坐标
  public void getOccupiedCoords(Intc2 consumer) {
    if (shapeOffsets != null) {
      for (int i = 0; i < shapeOffsets.length; i += 2) {
        consumer.get(tx + shapeOffsets[i], ty + shapeOffsets[i + 1]);
      }
    } else if (block != null) {
      int s = (int) block.size;
      for (int dx = 0; dx < s; dx++) {
        for (int dy = 0; dy < s; dy++) {
          consumer.get(tx + dx, ty + dy);
        }
      }
    }
  }

  /** 交互逻辑：判断是否占据指定坐标 */
  //参数是瓦片坐标
  public boolean occupies(int worldX, int worldY) {
    if (shapeOffsets != null) {
      for (int i = 0; i < shapeOffsets.length; i += 2) {
        if (tx + shapeOffsets[i] == worldX && ty + shapeOffsets[i + 1] == worldY) {
          return true;
        }
      }
      return false;
    }
    if (block != null) {
      int s = (int) block.size;
      return worldX >= tx && worldX < tx + s && worldY >= ty && worldY < ty + s;
    }
    return false;
  }

  @Override
  public void draw() {
    block.draw(this);
  }
  
  @Override
  public void kill() {
    // TODO: Implement this method
    remove();
  }
  

  @Override
  public void remove() {
    // 从世界数据移除
    if (WorldData.buildings != null) {
      WorldData.buildings.remove(this);
    }
    Pools.free(this);
  }

  @Override
  public void reset() {
    super.reset();
    block = null;
    shapeOffsets = null;
    tx = 0;
    ty = 0;
    angle = 0;
    id = Entities.freeID(this.id);
  }
  
  @Override
  public void write(Writes w) {
    // TODO: Implement this method
  }
  
  @Override
  public void read(Reads r) {
    // TODO: Implement this method
  }
  

  // --- 静态工厂方法 ---
  public static Building create(Block block, int tx, int ty, int angle) {
    Building building = Pools.obtain(Building.class, Building::new);
    building.block = block;
    building.tx = tx;
    building.ty = ty;
    building.angle = angle;
    building.init();
    return building;
  }

  public static Building create(Block block, int tx, int ty) {
    return create(block, tx, ty, 0);
  }
}