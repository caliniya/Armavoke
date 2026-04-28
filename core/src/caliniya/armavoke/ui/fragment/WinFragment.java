package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Graphics;
import arc.graphics.Color;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.scene.style.NinePatchDrawable;
import arc.util.Align;
import caliniya.armavoke.ui.Button;

public class WinFragment {

  public Table root; // 定位节点
  public Table window; // 窗口节点
  public Table main; // 给子类用的内容填充区
  
  public float w = 400f, h = 300f; // 默认大小
  public String title = "Window"; // 标题

  public void build() {
    if (root != null) root.remove();
    root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.enabled; // 确保根节点可接收事件

    window = new Table();
    main = new Table();
    main(main);

    window.setBackground(
        new NinePatchDrawable((NinePatchDrawable) Core.atlas.getDrawable("Window")));

    window.touchable = Touchable.enabled;
    window.cullable = true;

    Table titleTable = new Table();
    titleTable.add(new Label(title)).color(Color.white).expandX().left().padLeft(10f).touchable(Touchable.enabled).growX();

    Button closeBtn = new Button("@close", () -> remove());

    titleTable.add(closeBtn).align(Align.topRight); // 调整对齐方式
    
    // --- 新增：拖动监听器 ---
    titleTable.addListener(new InputListener() {
      // 记录点击时鼠标相对于窗口左下角的偏移量
      float grabX, grabY;

      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, arc.input.KeyCode button) {
        // 将标题栏内的坐标转换为舞台坐标，再转换为窗口的本地坐标
        // event.stageX/Y 是鼠标在舞台上的绝对坐标
        // window.x/y 是窗口在舞台上的坐标
        grabX = event.stageX - window.x;
        grabY = event.stageY - window.y;
        
        // 点击时将窗口置顶
        window.toFront();
        return true; // 必须返回 true 才能接收后续 drag 事件
      }

      @Override
      public void touchDragged(InputEvent event, float x, float y, int pointer) {
        // 计算新位置：鼠标当前舞台坐标 - 之前记录的偏移量
        float newX = event.stageX - grabX;
        float newY = event.stageY - grabY;

        // 可选：限制窗口不被拖出屏幕
        // float parentWidth = Core.scene.getWidth();
        // float parentHeight = Core.scene.getHeight();
        // newX = Mathf.clamp(newX, 0, parentWidth - window.getWidth());
        // newY = Mathf.clamp(newY, 0, parentHeight - window.getHeight());

        window.setPosition(newX, newY);
      }
    });
    // -----------------------

    window.add(titleTable).growX().align(Align.topRight);
    window.row();

    window.image().color(Color.valueOf("98BFF5FF")).fillX().height(2f);
    window.row();

    window.add(main).grow().pad(10f);

    root.add(window).size(w, h);
    // 默认居中显示
    root.getCell(window).center();
    
    Core.scene.root.addChild(root);
    
    // 构建完成后将窗口居中 (模拟 Dialog 的行为)
    Core.app.post(() -> {
        window.setPosition(
            (Core.scene.getWidth() - window.getWidth()) / 2,
            (Core.scene.getHeight() - window.getHeight()) / 2
        );
    });
  }

  /** 销毁窗口 */
  public void remove() {
    root.remove();
  }

  public void main(Table t) {}
}