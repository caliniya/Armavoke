package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.Table;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Unit;

/**
 * 指挥信息窗口：列出所有友军单位的具体信息。
 *
 * <p>目前硬编码显示血量/护盾/护甲/能量； 未来由 Entity 上的"展示自身信息"方法提供。
 */
public class CommandInfoWindow extends Window {

  public CommandInfoWindow() {
    super("指挥信息");
  }

  @Override
  public void main(Table t) {
    int[] count = {0};
    WorldData.units.each(
        u -> {
          if (u == null || u.team != Game.team) return;
          count[0]++;
          t.add(
                  "[light]"
                      + u.type.name
                      + "[] 血="
                      + (int) u.health
                      + " 盾="
                      + (int) u.totalShield()
                      + " 甲="
                      + (int) u.armor
                      + " 能="
                      + (int) u.energy)
              .left()
              .pad(2f)
              .row();
        });
    if (count[0] == 0) {
      t.add("[gray]没有友军单位[]").pad(10f);
    }
  }
}
