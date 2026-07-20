package caliniya.armavoke.base.game;

import arc.math.geom.QuadTree.QuadTreeObject;
import arc.math.geom.Rect;
import arc.util.pooling.Pool.Poolable;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.base.type.TeamTypes;
import arc.util.io.*;

/**
 * 游戏实体基类。
 * 实现了 {@link QuadTreeObject} 以便放入 EntityGroup 的四叉树空间索引。
 */
public abstract class Entity implements Poolable, QuadTreeObject {

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

  public Entity() {}

  public abstract void update(float dt);
  public abstract void draw();
  public abstract void remove();
  public abstract void kill();
  public abstract void write(Writes w);
  public abstract void read(Reads r);

  /**
   * 返回实体的碰撞盒尺寸（直径）。
   * 子类应该覆盖此方法以提供准确的碰撞体大小。
   * 默认返回 8 像素。
   */
  public float hitboxSize() {
    return 8f;
  }

  /**
   * 填充实体的粗略包围盒。
   * 该包围盒不能小于实体实际范围，但可以偏大。
   */
  @Override
  public void hitbox(Rect out) {
    float half = hitboxSize() / 2f;
    out.set(x - half, y - half, hitboxSize(), hitboxSize());
  }

  @Override
  public void reset() {
    x = 0;
    y = 0;
    health = 0;
    maxHealth = 0;
    team = null;
    item = null;
    target = null;
  }
}
