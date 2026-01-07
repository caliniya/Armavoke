package caliniya.armavoke.system.game;

import arc.util.Time;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;

public class GameProcess extends BasicSystem<GameProcess> {

  @Override
  public GameProcess init() {
    index = 2;
    return super.init(false);
  }

  @Override
  public void update() {

    Ar<Unit> units = WorldData.units;
    for (int i = 0; i < units.size; i++) {
      Unit u = units.get(i);
      
      if (u == null || u.health <= 0) continue;

      u.update(Time.delta);
      u.updateWeapons(Time.delta);
    }
  }
}