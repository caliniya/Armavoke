package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.OS;
import arc.util.Time;
import caliniya.armavoke.Init;
import caliniya.armavoke.game.data.WorldData;

public class DebugFragment {

  private Table table;

  public void add() {
    if (table != null && table.parent != null) return;

    table = new Table();
    table.setFillParent(true);
    table.touchable = Touchable.disabled;
    table.top().right();

    table
        .table(
            t -> {
              t.label(
                      () -> {
                        // 基础系统信息 (始终可显示)
                        StringBuilder sb = new StringBuilder();
                        sb.append("FPS: ").append(Core.graphics.getFramesPerSecond()).append("\n");
                        sb.append("Mem: ")
                            .append(Core.app.getJavaHeap() / 1024 / 1024)
                            .append("  +  ")
                            .append(Runtime.getRuntime().totalMemory() / 1024 / 1024)
                            .append("  +  ")
                            .append(Runtime.getRuntime().freeMemory() / 1024 / 1024)
                            .append("  all  ")
                            .append(" MB\n");

                        // 世界数据检查
                        if (WorldData.world == null) {
                          sb.append("World Data: null\n");
                        } else {

                          int unitCount = (WorldData.units != null) ? WorldData.units.size : 0;
                          int moveUnitCount =
                              (WorldData.moveunits != null) ? WorldData.moveunits.size : 0;

                          sb.append("Units: ").append(unitCount).append("\n");
                          sb.append("Moving: ").append(moveUnitCount).append("\n");
                          sb.append("Map: ")
                              .append(WorldData.world.W)
                              .append("x")
                              .append(WorldData.world.H)
                              .append("\n");
                        }

                        // sb.append("timedelta").append(Time.delta);

                        sb.append("Java: ").append(OS.javaVersion);
                        sb.append("Android: ").append(Core.app.getVersion());

                        return sb.toString();
                      })
                  .color(Color.white);
            })
        .margin(10f);

    Core.scene.add(table);
  }

  public void remove() {
    if (table != null) {
      table.remove();
      table = null;
    }
  }
}
