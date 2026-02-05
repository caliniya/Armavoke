package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.NinePatch;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.scene.style.NinePatchDrawable;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Align;
import caliniya.armavoke.ui.Button;

public abstract class WinFragment {

  public Table root; //定位节点
  public Table window; // 窗口节点
  public Table main; // 给子类用的内容填充区

  public float w = 400f, h = 300f; // 默认大小
  public String title = "Window"; // 标题

  public void build() {
    root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.childrenOnly;

    window = new Table();

    window.setBackground(
        new NinePatchDrawable((NinePatchDrawable) Core.atlas.getDrawable("Window")));

    window.touchable = Touchable.enabled;

    Table titleTable = new Table();
    titleTable.add(new Label(title)).color(Color.white).expandX().left().padLeft(10f);

    Button closeBtn = new Button("@close", () -> this.remove());
    
    window.add(titleTable).growX().align(Align.topRight);
    titleTable.add(closeBtn).growX().align(Align.topLeft).scrollX(true);
    window.row();

    window.image().color(Color.valueOf("98BFF5FF")).fillX().height(2f);
    window.row();

    window.add(main).grow().pad(10f);

    root.add(window).size(w, h).center();

    Core.scene.root.addChild(root);
  }

  /** 销毁窗口 */
  public void remove() {
    if (root != null) {
      root.remove();
      root = null;
    }
  }

  /** 动态修改标题 */
  public void setTitle(String newTitle) {
    this.title = newTitle;
  }
}
