package caliniya.armavoke.base.game;

import arc.util.pooling.Pool.Poolable;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.base.type.TeamTypes; // 导入
import arc.util.io.*;

public abstract class Entity implements Poolable {

  // --- 公共坐标 ---
  public float x, y;

  // --- 公共状态 ---
  public float health;
  public float maxHealth;
  public int id;
  public TeamTypes team;

  // --- 公共组件 ---
  public ItemModule item;

  public Entity() {}

  /** 核心逻辑更新 */
  public abstract void update(float dt);

  /** 渲染逻辑 */
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
    team = null; // 重置阵营
    item = null;
  }
}