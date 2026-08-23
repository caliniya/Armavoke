package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.Table;
import caliniya.armavoke.core.UI;

public class PauseWindow extends Window {
  public PauseWindow() { super("暂停"); }

  @Override
  public void main(Table table) {
    table.defaults().growX().pad(5f);
    table.button("继续", this::remove).row();
    table.button("返回主菜单", () -> { remove(); UI.Menu(); }).row();
  }
}
