package caliniya.armavoke.game;

import arc.func.Intc2;
import arc.util.io.*;
import arc.util.pooling.Pools;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.type.module.*;
import caliniya.armavoke.world.*;
import caliniya.armavoke.base.game.*;
import caliniya.armavoke.game.*;

public class Building extends Entity {

  // --- 锚点坐标 (左下角起始点) ---
  // 0:上, 1:右, 2:下, 3:左
  public int tx, ty, angle;

  public float rotation; // 实际渲染旋转角度 (精确到度，用于炮塔转动)
  public Unit target; // 当前攻击目标 (运行时状态，不需要存档)
  public float reload; // 武器装填进度

  // --- 形状数据 (独立副本，已旋转) ---
  public int[] shapeOffsets;

  public Block block;

  protected Building() {}

  /** 初始化建筑实体 */
  public void init() {
    // 确保坐标和角度有效
    if (block == null) return;

    this.angle = angle % 4;

    // 初始化坐标
    this.x = tx * WorldData.TILE_SIZE;
    this.y = ty * WorldData.TILE_SIZE;

    // 初始化血量
    this.maxHealth = block.health;
    // 只有当血量为0时才初始化为满血(防止覆盖读取存档后的数据)
    if (this.health <= 0) this.health = block.health;

    // 初始化物品
    if (this.item == null) {
      this.item = new ItemModule(block.capacity);
      this.item.setFilter(block.allowItem);
    }

    // 分配 ID
    if (this.id <= 0) this.id = Entities.assignID();

    // 计算旋转后的形状数据
    if (block.shapeOffsets != null) {
      this.shapeOffsets = Block.getRotatedOffsets(this.angle, block.shapeOffsets);
    } else {
      this.shapeOffsets = null;
    }
  }

  @Override
  public void update(float dt) {
    block.update(this, dt);
  }

  /** 计算该建筑占据的所有世界坐标 */
  public void getOccupiedCoords(Intc2 consumer) {
    if (shapeOffsets != null) {
      for (int i = 0; i < shapeOffsets.length; i += 2) {
        consumer.get(tx + shapeOffsets[i], ty + shapeOffsets[i + 1]);
      }
    } else if (block != null) {
      int s = block.size;
      for (int dx = 0; dx < s; dx++) {
        for (int dy = 0; dy < s; dy++) {
          consumer.get(tx + dx, ty + dy);
        }
      }
    }
  }

  /** 交互逻辑：判断是否占据指定坐标 */
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
      int s = block.size;
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
    remove();
  }

  @Override
  public void remove() {
    if (WorldData.buildings != null) {
      WorldData.buildings.remove(this);
    }
    // 归还 ID
    if (id > 0) {
      Entities.freeID(id);
      id = -1;
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
    rotation = 0;
    reload = 0;
    target = null;
  }

  /** 写入存档数据 顺序：BlockID -> 坐标/朝向 -> 实体状态 -> 动态状态 -> 模块数据 */
  @Override
  public void write(Writes w) {

    w.i(tx);
    w.i(ty);
    w.b((byte) angle); // 0-3 只需要一个字节

    // 3. 写入实体状态
    w.f(health);
    w.b((byte) team.ordinal()); // 阵营序号

    block.write(this, w);

    // 5. 写入物品模块
    item.write(w);
  }

  /** 读取存档数据 注意：调用此方法前，对象通常是通过 Pools.obtain 获得的空对象 */
  @Override
  public void read(Reads r) {
    // 2. 读取基础位置信息
    this.tx = r.i();
    this.ty = r.i();
    this.angle = r.b();

    // 3. 读取实体状态
    this.health = r.f();
    byte teamID = r.b();
    this.team = TeamTypes.values()[teamID];

    block.read(this, r);

    if (this.item == null && block != null) {
      this.item = new ItemModule(block.capacity);
      this.item.setFilter(block.allowItem);
    }

    // 读取物品数据
    item.read(r);

    // 计算派生数据
    this.x = tx * WorldData.TILE_SIZE;
    this.y = ty * WorldData.TILE_SIZE;
    if (block != null) {
      this.maxHealth = block.health;
      if (block.shapeOffsets != null) {
        this.shapeOffsets = Block.getRotatedOffsets(this.angle, block.shapeOffsets);
      }
    }

    // 分配新 ID
    this.id = Entities.assignID();
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
