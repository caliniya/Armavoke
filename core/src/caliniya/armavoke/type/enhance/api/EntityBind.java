package caliniya.armavoke.type.enhance;

import caliniya.armavoke.base.game.Entity;

/** 绑定实体：强化实体自身属性（护甲抗性/能量/速度等）的模组实现此接口。 */
public interface EntityBind {

  /** 挂载时绑定实体（子类覆写做初始化/备份）。 */
  void bindEntity(Entity e);
}
