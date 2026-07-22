package caliniya.armavoke.system.game;

import arc.util.*;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.content.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.system.*;
import caliniya.armavoke.system.world.BulletProcess;

// 在这里进行主线程游戏内容的更新
public class GameProcess extends caliniya.armavoke.system.System<GameProcess> {

  public Ar<Unit> deadUnits;
  public Ar<Building> deadBuildings;
  public Ar<Entity> freshKilled; // 接收 BulletProcess 的即时击杀通知

  @Override
  public GameProcess init() {
    index = 2;
    deadUnits = new Ar<>();
    deadBuildings = new Ar<>();
    freshKilled = new Ar<>();
    return super.init(false);
  }

  @Override
  public void update() {
    // 先处理 BulletProcess 线程刚击杀的实体（延迟最小化，防止血量变负才死）
    Systems.BP.drainFreshKills(freshKilled);
    for (Entity e : freshKilled) {
      e.kill();
    }
    freshKilled.clear();
    
    // 不能在迭代器中进行写操作，因为这样会死锁
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
    deadUnits.clear();

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
    deadBuildings.clear();
  }
}
