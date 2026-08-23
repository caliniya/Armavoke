package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.Table;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.type.Enhancement;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.ability.Ability;

public class UnitDetailWindow extends Window {
  private final Unit unit;
  public StatStack stst;

  public UnitDetailWindow(Unit unit) {
    super(unit == null || unit.type() == null ? "单位" : unit.type().localizedName);
    this.unit = unit;
    this.stst = new StatStack();
  }

  @Override
  public void main(Table table) {
    if (unit == null || !unit.active()) {
      table.add("[red]单位已失效[]");
      return;
    }
    table.defaults().left().pad(3f);
    table.add("生命: " + (int) unit.health() + " / " + (int) unit.maxHealth()).row();
    table.add("护甲: " + (int) unit.armor() + " / " + (int) unit.maxArmor()).row();
    table.add("能量: " + (int) unit.energy() + " / " + (int) unit.energyMax()).row();
    table.add("热量: " + (int) unit.heat() + " / " + (int) unit.heatMax()).row();
    table.add("能力").padTop(8f).row();
    for (Ability ability : unit.abilities()) table.add("- " + ability.localizedName).row();
    table.add("强化").padTop(8f).row();
    for (Enhancement enhancement : unit.enhancements()) {
      table.add("- " + (enhancement.type == null ? "unknown" : enhancement.type.localizedName)).row();
    }
  }
}
