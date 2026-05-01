package caliniya.armavoke.system.game;

import arc.util.Log;
import arc.util.Time;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.content.Items;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;

public class GameProcess extends caliniya.armavoke.system.System<GameProcess> {

  @Override
  public GameProcess init() {
    index = 2;
    return super.init(false);
  }

  @Override
  public void update() {
    for (Unit u : WorldData.units) {
      
      if (u == null || u.health <= 0) continue;

      u.update(Time.delta);
      u.updateWeapons(Time.delta);
      u.item.addItem(Items.Ge,10);
      
    }
    WorldData.buildings.each(b -> b.update(Time.delta));
  }
}