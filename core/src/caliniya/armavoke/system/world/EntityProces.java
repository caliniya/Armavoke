package caliniya.armavoke.system.world;

import arc.util.Time;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;
import caliniya.armavoke.type.Weapon;

// 对所有实体进行处理
// 未来还会包括碰撞相关
// 在这里处理锁定，为实体指定目标，具体的开火逻辑在主线程运行
public class EntityProces extends caliniya.armavoke.system.System<EntityProces> {

  @Override
  public EntityProces init() {
    return super.init(true);
  }

  @Override
  public void update() {
    for (Unit u : WorldData.units) {
      if (u == null) continue;
      if (u.health <= 0) {
        u.kill();
        continue;
      }
      for(Weapon w : u.weapons){
        if (w.type.mirror) continue;
        //Entities.closestEnemy();
      }
    }
    for (Building b : WorldData.buildings) {
      if (b == null) continue;
      if (b.health <= 0) {
        b.kill();
        continue;
      }
    }
  }
}
