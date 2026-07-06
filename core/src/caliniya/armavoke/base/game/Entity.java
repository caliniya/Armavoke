package caliniya.armavoke.base.game;

import arc.util.pooling.Pool.Poolable;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.base.type.TeamTypes;
import arc.util.io.*;

public abstract class Entity implements Poolable {

  // --- 公共坐标 ---
  public float x, y ;

  // --- 公共状态 ---
  public float health;
  public float maxHealth;
  public int id;
  public TeamTypes team;

  // --- 公共组件 ---
  public ItemModule item;
  
  // 此实体所锁定的目标
  public Entity target;

  // 实体在空间网格中的区块索引，-1 表示未注册
  public int currentChunkIndex = -1;

  public Entity() {}

  public abstract void update(float dt);
  public abstract void draw();
  public abstract void remove();
  public abstract void kill();
  public abstract void write(Writes w);
  public abstract void read(Reads r);

  @Override
  public void reset() {
    x = 0;
    y = 0;
    health = 0;
    maxHealth = 0;
    team = null;
    item = null;
    target = null;
    currentChunkIndex = -1;
  }
}
