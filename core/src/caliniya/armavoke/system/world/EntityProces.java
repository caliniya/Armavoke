package caliniya.armavoke.system.world;

import arc.util.Time;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;

// 对所有实体进行处理
// 未来还会包括碰撞相关
public class EntityProces extends caliniya.armavoke.system.System<EntityProces> {
  
  @Override
  public EntityProces init() {
    return super.init(true);
  }

  @Override
  public void update(float dt) {
    for (int i = 0; i < WorldData.units.size; i++) {
      Unit u = WorldData.units.get(i);
      if (u == null) continue;
      if (u.health <= 0) {
          u.kill(); 
          continue;
      }
      
      // 暂时强制所有单位开火 (测试用)
      u.shooting = true;
    }
    for (Building b : WorldData.buildings) {
      if(b == null) continue;
      if(b.health <= 0) {
      	b.kill();
        continue;
      }
    }
  }
}