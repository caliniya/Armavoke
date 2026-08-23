package caliniya.armavoke.game.data;

import arc.func.Boolf;
import arc.func.Cons;
import arc.math.Mathf;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.type.Unit;

/** Selection and command state backed by ECS unit components. */
public final class CommandData {
  public static Ar<Unit> checkedUnits = new Ar<>();
  public static boolean commanding;
  public static boolean boxSelect;
  public static boolean boxDragging;
  public static float boxStartX, boxStartY, boxEndX, boxEndY;

  public enum CommandType { None, Move, Stop }
  public static CommandType commandType = CommandType.Move;

  private CommandData() {}

  public static void init() {
    clearSelection();
    commanding = false;
    boxSelect = false;
    boxDragging = false;
    commandType = CommandType.Move;
  }

  public static void clearSelection() {
    for (Unit unit : checkedUnits) if (unit != null && unit.active()) unit.selected(false);
    checkedUnits.clear();
  }

  public static boolean select(Unit unit) {
    if (unit == null || !unit.active() || unit.health() <= 0f || unit.team() != Game.team) return false;
    if (checkedUnits.addUnique(unit)) unit.selected(true);
    return true;
  }

  public static void replaceSelection(Ar<Unit> units) {
    clearSelection();
    if (units != null) for (Unit unit : units) select(unit);
  }

  public static Unit findUnitAt(float x, float y, float radius) {
    Unit result = null;
    float best = radius * radius;
    for (Unit unit : EcsQueries.units()) {
      if (!unit.active() || unit.health() <= 0f) continue;
      float distance = Mathf.dst2(x, y, unit.x(), unit.y());
      if (distance <= best) { best = distance; result = unit; }
    }
    return result;
  }

  public static void findUnit(float x, float y, Boolf<Unit> filter, Cons<Unit> consumer) {
    Unit unit = findUnitAt(x, y, 100f);
    if (unit != null && (filter == null || filter.get(unit))) consumer.get(unit);
  }

  public static void findUnit(float x, float y, Cons<Unit> consumer) {
    findUnit(x, y, unit -> true, consumer);
  }
}
