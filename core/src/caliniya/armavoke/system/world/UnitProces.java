package caliniya.armavoke.system.world;

import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;

public class UnitProces extends caliniya.armavoke.system.System<UnitProces> {

  
  @Override
  public UnitProces init() {
    return super.init(true);
  }

  @Override
  public void update() {
    Ar<Unit> list = WorldData.units;

    for (int i = 0; i < list.size; i++) {
      Unit u = list.get(i);
      if (u == null) continue;
      if (u.health <= 0) {
          u.remove(); 
          continue;
      }
      
      // 暂时强制所有单位开火 (测试用)
      u.shooting = true;
    }
  }
}