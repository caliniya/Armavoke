package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.OS;
import arc.util.Time;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.data.CommandData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.ui.Styles;
import java.util.Arrays;

/** Lightweight runtime diagnostics, refreshed four times per second. */
public class DebugFragment {
  private static final float refreshInterval = 15f;
  private static final TeamTypes[] teamValues = TeamTypes.values();

  private final StringBuilder text = new StringBuilder(512);
  private final int[] teamUnits = new int[teamValues.length];
  private final int[] teamBuildings = new int[teamValues.length];

  private Table root;
  private String cachedText = "";
  private float nextRefresh;

  public void add() {
    if (root != null && root.parent != null) return;

    root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.disabled;
    root.top().right();

    root.table(
            panel -> {
              panel.background(Styles.background);
              panel.setColor(new Color(0.16f, 0.19f, 0.22f, 0.82f));
              panel.left();
              panel.label(this::debugText).color(Color.white).left();
            })
        .margin(10f)
        .pad(8f);

    Core.scene.add(root);
  }

  private String debugText() {
    if (!cachedText.isEmpty() && Time.time < nextRefresh) return cachedText;
    nextRefresh = Time.time + refreshInterval;
    text.setLength(0);

    text.append("View: ").append(UI.currentView).append('\n');
    text.append("FPS: ").append(Core.graphics.getFramesPerSecond()).append('\n');
    text.append("Heap: ").append(Core.app.getJavaHeap() / 1024 / 1024).append(" MB\n");

    if (WorldData.world == null) {
      text.append("World: null\n");
    } else {
      collectTeamCounts();
      int moving = moveUnitCount();
      int bullets = bulletCount();
      text.append("Map: ")
          .append(WorldData.world.W)
          .append('x')
          .append(WorldData.world.H)
          .append('\n');
      text.append("Units: ")
          .append(WorldData.units == null ? 0 : WorldData.units.size())
          .append("  Moving: ")
          .append(moving)
          .append('\n');
      text.append("Buildings: ")
          .append(WorldData.buildings == null ? 0 : WorldData.buildings.size())
          .append("  Bullets: ")
          .append(bullets)
          .append('\n');
      text.append("Selected: ")
          .append(CommandData.checkedUnits.size)
          .append("  Command: ")
          .append(CommandData.commandType)
          .append('\n');

      for (int i = 0; i < teamValues.length; i++) {
        text.append(teamValues[i])
            .append(": U")
            .append(teamUnits[i])
            .append(" B")
            .append(teamBuildings[i])
            .append('\n');
      }
    }

    text.append("TPS BP/EP/UM: ")
        .append(tps(Systems.BP))
        .append('/')
        .append(tps(Systems.EP))
        .append('/')
        .append(tps(Systems.UM))
        .append('\n');
    text.append("Java: ").append(OS.javaVersion);
    if (OS.isAndroid) text.append("  Android: ").append(Core.app.getVersion());

    cachedText = text.toString();
    return cachedText;
  }

  private void collectTeamCounts() {
    Arrays.fill(teamUnits, 0);
    Arrays.fill(teamBuildings, 0);
    if (WorldData.units != null) {
      for (Unit unit : WorldData.units) {
        if (unit != null && unit.team != null) teamUnits[unit.team.ordinal()]++;
      }
    }
    if (WorldData.buildings != null) {
      for (Building building : WorldData.buildings) {
        if (building != null && building.team != null) teamBuildings[building.team.ordinal()]++;
      }
    }
  }

  private int moveUnitCount() {
    if (WorldData.moveunits == null) return 0;
    synchronized (WorldData.moveunits) {
      return WorldData.moveunits.size();
    }
  }

  private int bulletCount() {
    if (WorldData.bullets == null) return 0;
    if (Systems.BP == null) return WorldData.bullets.size();
    synchronized (Systems.BP.BULLET_LOCK) {
      return WorldData.bullets.size();
    }
  }

  private int tps(caliniya.armavoke.system.System<?> system) {
    return system == null ? 0 : Math.round(system.smoothedTps);
  }

  public void remove() {
    if (root != null) {
      root.remove();
      root = null;
    }
  }
}
