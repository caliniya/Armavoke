package caliniya.armavoke.game;

import arc.func.Intc2;
import arc.util.Log;
import arc.util.io.*;
import arc.util.pooling.Pools;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.type.module.*;
import caliniya.armavoke.world.*;
import caliniya.armavoke.base.game.*;

public class Building extends Entity {

  // --- 锚点坐标 (左下角起始点) ---
  // 0:上, 1:右, 2:下, 3:左
  public int tx, ty, angle;

  public TeamData teamData;

  public float rotation; // 实际渲染旋转角度 (精确到度，用于炮塔转动)
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
    this.x = (tx * WorldData.TILE_SIZE) + block.psize / 2;
    this.y = (ty * WorldData.TILE_SIZE) + block.psize / 2;

    // 初始化血量
    this.maxHealth = block.health;
    // 只有当血量为0时才初始化为满血(防止覆盖读取存档后的数据)
    if (this.health <= 0) this.health = block.health;

    // 初始化物品
    if (this.item == null) {
      this.item = new ItemModule(block.capacity);
      this.item.setFilter(block.allowItem);
    }

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
    // 从团队空间网格注销
    Teams.remove(this);
    if (this.team != null && currentChunkIndex != -1) {
      TeamData td = Teams.get(this.team);
      if (td != null && td.entityGrid != null && currentChunkIndex < td.entityGrid.length) {
        td.entityGrid[currentChunkIndex].remove(this);
      }
    }

    WorldData.buildings.remove(this);
    WorldData.world.removeBuilding(tx, ty);
    // 归还 ID
    id = Entities.freeID(id);

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

  /** 写入存档数据 */
  @Override
  public void write(Writes w) {
    w.b((byte) angle); // 0-3 只需要一个字节
    w.i(tx);
    w.i(ty);
    w.i(angle);
    w.f(health);
    w.b((byte) team.ordinal()); // 阵营序号
    w.s(id);

    block.write(this, w);
    item.write(w);
  }

  /** 读取存档数据 */
  @Override
  public void read(Reads r) {
    this.angle = r.b();

    this.tx = r.i();
    this.ty = r.i();
    this.angle = r.i();

    this.health = r.f();
    byte teamID = r.b();
    this.team = TeamTypes.values()[teamID];
    this.id = Entities.checkoutID(r.s());

    block.read(this, r);

    if (this.item == null && block != null) {
      this.item = new ItemModule(block.capacity);
      this.item.setFilter(block.allowItem);
    }

    item.read(r);

    this.x = tx * WorldData.TILE_SIZE + block.psize / 2;
    this.y = ty * WorldData.TILE_SIZE + block.psize / 2;
    if (block != null) {
      this.maxHealth = block.health;
      if (block.shapeOffsets != null) {
        this.shapeOffsets = Block.getRotatedOffsets(this.angle, block.shapeOffsets);
      }
    }

    Teams.add(this);
    teamData = team.data();
    teamData.updateChunk(this, -1, WorldData.getChunkIndex(x, y));
  }

  // --- 静态工厂方法 ---
  public static Building create(Block block, int tx, int ty, int angle, TeamTypes team) {
    Building building = Pools.obtain(Building.class, Building::new);
    building.block = block;
    building.tx = tx;
    building.ty = ty;
    building.angle = angle;
    building.team = team;
    Teams.add(building);
    building.teamData = team.data();
    building.teamData.updateChunk(building, -1, WorldData.getChunkIndex(building.x, building.y));
    building.init();
    building.id = Entities.assignID();
    WorldData.buildings.add(building);
    return building;
  }

  public static Building create(Block block, int tx, int ty, TeamTypes team) {
    return create(block, tx, ty, 0, team);
  }

  public static Building create(Block block) {
    Building building = Pools.obtain(Building.class, Building::new);
    building.block = block;
    building.init();
    return building;
  }
}
