package caliniya.armavoke.game.data;

import arc.func.Boolc;
import arc.func.Boolf;
import arc.func.Boolp;
import arc.func.Cons;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.game.Unit;

public class CommandData {
  // 包括UI需要的建筑和单位，资源相关数据。

  // 当前选中的单位
  public static Ar<Unit> checkedUnits = new Ar<Unit>();
  public static boolean commanding;

  // 初始化，也包括重置数据
  public static void init() {
    checkedUnits.clear();
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
