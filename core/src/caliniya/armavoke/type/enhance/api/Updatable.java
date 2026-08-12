package caliniya.armavoke.type.enhance;

import caliniya.armavoke.base.game.Entity;

/** 每帧逻辑：需要持续运转的模组（感应扫描、侧向推进等）实现此接口。 */
public interface Updatable {

  /** 每帧更新（由实体的 updatable 列表遍历调用）。 */
  void update(Entity e, float dt);
}
