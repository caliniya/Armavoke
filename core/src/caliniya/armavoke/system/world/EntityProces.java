package caliniya.armavoke.system.world;

import arc.math.Mathf;
import arc.util.Log;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;
import caliniya.armavoke.type.Weapon;

/**
 * 实体处理系统，运行在独立线程（60TPS）。
 *
 * <p>在这里处理索敌锁定，为实体指定目标。
 * 底层网格已通过 {@code TeamData.gridLock} 实现线程安全。
 * 具体的开火逻辑在主线程运行。
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

        float wx = u.x + w.type.x;
        float wy = u.y + w.type.y;

        if (w.rotate) {
          // 旋转武器（炮塔）：独立锁敌
          // 目标失效 或 超出射程 → 重搜
          if (w.target == null || w.target.health <= 0
              || Mathf.dst2(wx, wy, w.target.x, w.target.y) > w.type.range * w.type.range) {
            w.type.findTarget(w, wx, wy);
          }
        } else {
          // 固定武器：直接瞄准单位锁定的目标
          w.target = u.target;
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

      if (b.target == null || b.target.health <= 0) {
        b.target = b.block.findTarget(b);
      }
    }
  }
}
