package caliniya.armavoke.base.game;

import arc.util.pooling.Pool.Poolable;
import caliniya.armavoke.type.module.ItemModule;
import arc.util.io.*;

public abstract class Entity implements Poolable {

  // --- 公共坐标 ---
  public float x, y;

  // --- 公共状态 ---
  public float health;
  public float maxHealth; // 建筑需要，单位也可以通过类型初始化此值
  public int id;

  // --- 公共组件 ---
  public ItemModule item;

  public Entity() {}

  /** 核心逻辑更新 */
  public abstract void update(float dt);

  /** 渲染逻辑 */
  public abstract void draw();
  
  /** 从世界移除 没有回调的*/
  public abstract void remove();
  
  //带回调的
  public abstract void kill();
  
  public abstract void write(Writes w);
  
  public abstract void read(Reads r);

  @Override
  public void reset() {
    x = 0;
    y = 0;
    health = 0;
    maxHealth = 0;
    item = null;
  }
}