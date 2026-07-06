package caliniya.armavoke.system.game;

import arc.util.Log;
import arc.util.Time;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.content.Items;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;

// 在这里进行主线程游戏内容的更新
public class GameProcess extends caliniya.armavoke.system.System<GameProcess> {

  @Override
  public GameProcess init() {
    index = 2;
    return super.init(false);
  }

  @Override
  public void update() {
    WorldData.units.each(
        u -> {
          if (u != null || u.health <= 0) {
            u.update(Time.delta);
            u.updateWeapons(Time.delta);
          }
        });
    WorldData.buildings.each(
        b -> {
          if (b != null || b.health <= 0) {
            b.update(Time.delta);
          }
        });
  }
}
