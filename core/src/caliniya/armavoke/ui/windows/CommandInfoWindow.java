package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.Table;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.type.Unit;

public class CommandInfoWindow extends Window {
  public CommandInfoWindow() { super("单位列表"); }

  @Override
  public void main(Table table) {
    table.defaults().left().pad(2f);
    for (Unit unit : EcsQueries.units()) {
      if (unit.team() != Game.team || unit.type() == null) continue;
      table.add(unit.type().name + "  HP " + (int) unit.health() + "/" + (int) unit.maxHealth()
          + "  Armor " + (int) unit.armor() + "  Energy " + (int) unit.energy()).row();
    }
  }
}
