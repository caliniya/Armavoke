package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.game.data.CommandData;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.ui.windows.CommandInfoWindow;
import caliniya.armavoke.ui.windows.UnitDetailWindow;

/** Compact ECS command HUD. */
public class HUDFragment {
  public Table a, b;

  public void build() {
    a = new Table();
    b = new Table();
    a.setFillParent(true);
    a.bottom().left();
    a.defaults().pad(4f);
    a.button("指挥", () -> CommandData.commanding = !CommandData.commanding);
    a.button("框选", () -> CommandData.boxSelect = !CommandData.boxSelect);
    a.button("移动", () -> CommandData.commandType = CommandData.CommandType.Move);
    a.button("停止", () -> {
      CommandData.commandType = CommandData.CommandType.Stop;
      for (Unit unit : CommandData.checkedUnits) unit.stop();
    });
    a.button("单位", () -> new CommandInfoWindow().build());
    a.row();
    a.add(b).colspan(5).left();
    a.update(this::refreshCommand);
    Core.scene.root.addChild(a);
  }

  public void hideHUD() { if (a != null) a.visible = false; }
  public void showHUD() { if (a != null) a.visible = true; }

  public void refreshCommand() {
    if (b == null) return;
    b.clearChildren();
    b.label(() -> "已选 " + CommandData.checkedUnits.size + " 个单位").left();
    if (CommandData.checkedUnits.size == 1) {
      Unit unit = CommandData.checkedUnits.first();
      b.button(unit.type() == null ? "详情" : unit.type().name, () -> new UnitDetailWindow(unit).build());
    }
  }
}
