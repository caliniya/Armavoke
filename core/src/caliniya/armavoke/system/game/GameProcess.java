package caliniya.armavoke.system.game;

import arc.util.*;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.content.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.system.*;

// 在这里进行主线程游戏内容的更新
public class GameProcess extends caliniya.armavoke.system.System<GameProcess> {
  
  public Ar<Unit> deadUnits ;
  public Ar<Building> deadBuildings;
  
  @Override
  public GameProcess init() {
    index = 2;
    deadUnits = new Ar<>();
    deadBuildings = new Ar<>();
    return super.init(false);
  }

  @Override
  public void update() {
    // 先收集所有待击杀的实体，统一在迭代结束后处理
    // 避免 each() 迭代中 swap-with-last 删除导致元素被跳过
    deadUnits.clear();
    WorldData.units.each(
        u -> {
          if (u == null) return;
          if (u.health <= 0) {
            deadUnits.add(u);
            return;
          } else {
            u.update(Time.delta);
            u.canShoot = true;
            u.updateWeapons(Time.delta);
          }
        });
    for (Unit u : deadUnits) {
      u.kill();
    }

    deadBuildings.clear();
    WorldData.buildings.each(
        b -> {
          if (b == null) return;
          if (b.health <= 0) {
            deadBuildings.add(b);
            return;
          } else {
            b.update(Time.delta);
          }
        });
    for (Building b : deadBuildings) {
      b.kill();
    }
  }
}
