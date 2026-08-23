package caliniya.armavoke.game.data;

import arc.func.Boolc;
import arc.func.Boolf;
import arc.func.Boolp;
import arc.func.Cons;
import arc.math.Mathf;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.*;
import caliniya.armavoke.type.*;

public class CommandData {
  // 包括UI需要的建筑和单位，资源相关数据。

  // 当前选中的单位
  public static Ar<Unit> checkedUnits = new Ar<Unit>();
  public static boolean commanding;

  /** 是否启用覆盖式框选。 */
  public static boolean boxSelect;

  /** 当前框选手势及其屏幕坐标，供输入与 HUD 共享。 */
  public static boolean boxDragging;
  public static float boxStartX, boxStartY, boxEndX, boxEndY;

  /** 指挥状态（直接指挥行单选）。 */
  public enum CommandType {
    None, Move, Stop
  }

  /** 当前指挥状态（默认移动模式）。 */
  public static CommandType commandType = CommandType.Move;

  // 初始化，也包括重置数据
  public static void init() {
    clearSelection();
    commanding = false;
    boxSelect = false;
    boxDragging = false;
    commandType = CommandType.Move;
  }

  /** 清空选择并同步单位的选中标记。 */
  public static void clearSelection() {
    for (Unit unit : checkedUnits) {
      if (unit != null) unit.isSelected = false;
    }
    checkedUnits.clear();
  }

  /** 单点选择只增加，不会取消已经选中的单位。 */
  public static boolean select(Unit unit) {
    if (unit == null || unit.health <= 0 || unit.team != Game.team) return false;
    if (!checkedUnits.contains(unit)) {
      checkedUnits.add(unit);
      unit.isSelected = true;
    }
    return true;
  }

  /** 用一组单位覆盖当前选择；空数组等价于快速清空。 */
  public static void replaceSelection(Ar<Unit> units) {
    clearSelection();
    if (units == null) return;
    for (Unit unit : units) select(unit);
  }

  /** 在所有阵营中查找点击位置附近最近的单位。 */
  public static Unit findUnitAt(float x, float y, float radius) {
    if (WorldData.units == null) return null;

    final Unit[] result = {null};
    final float[] nearest = {radius * radius};
    WorldData.units.intersect(
        x - radius,
        y - radius,
        radius * 2f,
        radius * 2f,
        unit -> {
          if (unit == null || unit.health <= 0) return;
          float dst2 = Mathf.dst2(x, y, unit.x, unit.y);
          if (dst2 <= nearest[0]) {
            nearest[0] = dst2;
            result[0] = unit;
          }
        });
    return result[0];
  }

  //
  public static void findUnit(float x, float y, Boolf<Unit> F, Cons<Unit> C) {
    Game.team
        .data()
        .find(
            x,
            y,
            100f,
            e -> {
              if (e instanceof Unit) {
                if (F.get((Unit) e)) {
                  C.get((Unit) e);
                }
              }
            });
  }

  public static void findUnit(float x, float y, Cons<Unit> C) {
    findUnit(
        x,
        y,
        e -> {
          return true;
        },
        e -> C.get(e));
  }
}
