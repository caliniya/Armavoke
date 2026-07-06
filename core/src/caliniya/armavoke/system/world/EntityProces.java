package caliniya.armavoke.system.world;

import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;
import caliniya.armavoke.type.Weapon;

/**
 * 实体处理系统，运行在独立线程（60TPS）。
 *
 * <p>在这里处理索敌锁定，为实体指定目标。 底层网格已通过 {@code TeamData.gridLock} 实现线程安全， 可直接调用 {@code
 * Entities.closestEnemy()} 等 API。 具体的开火逻辑在主线程运行。
 */
public class EntityProces extends System<EntityProces> {

  @Override
  public EntityProces init() {
    return super.init(true);
  }

  @Override
  public void update() {
    // --- 单位处理 ---
    for (Unit u : WorldData.units) {
      if (u == null) continue;
      if (u.health <= 0) {
        u.kill();
        continue;
      }

      for (Weapon w : u.weapons) {
        if (w.type.mirror) continue;

        float wx = u.x + w.type.x;
        float wy = u.y + w.type.y;

        // 锁定：目标有效不重搜
        if (u.target == null || u.target.health <= 0) {
          w.type.findTarget(u, wx, wy);
        }
      }
    }

    // --- 建筑处理 ---
    for (Building b : WorldData.buildings) {
      if (b == null) continue;
      if (b.health <= 0) {
        b.kill();
        continue;
      }

      // 锁定：目标有效不重搜（射程校验由各 Block.update 负责）
      if (b.target == null || b.target.health <= 0) {
        b.target = b.block.findTarget(b);
      }
    }
  }
}
