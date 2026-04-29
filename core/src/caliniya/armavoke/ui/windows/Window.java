package caliniya.armavoke.ui.windows;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.NinePatchDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import caliniya.armavoke.ui.Button;

public class Window {

  public Table window;
  public Table main, top , low;

  public float w = 400f, h = 300f;
  public float maxOut = 0.8f; // 允许80%移出屏幕
  public String title = "Window";

  public Window() {
    this("");
  }

  public Window(String titleText) {
    this.title = titleText;
  }

  public void build() {
    if (window != null) window.remove();

    window = new Table();
    window.setSize(w, h); // 强制设定大小

    window.setBackground(
        new NinePatchDrawable((NinePatchDrawable) Core.atlas.getDrawable("Window")));

    window.touchable = Touchable.enabled;
    window.cullable = true;

    //容器
    main = new Table();
    top = new Table();
    low = new Table();
    
    top(top);
    main(main);
    low(low);
    ScrollPane scrollPane = new ScrollPane(main);
    scrollPane.setFadeScrollBars(true); 
    scrollPane.setScrollingDisabled(true, false);
    
    Table titleTable = new Table();
    Label titleLabel = new Label(title);
    titleLabel.setColor(Color.white);

    // 拖动监听
    titleLabel.addListener(new InputListener() {
      float grabX, grabY;

      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, arc.input.KeyCode button) {
        grabX = event.stageX - window.x;
        grabY = event.stageY - window.y;
        window.toFront();
        return true;
      }

      @Override
      public void touchDragged(InputEvent event, float x, float y, int pointer) {
        float newX = event.stageX - grabX;
        float newY = event.stageY - grabY;

        float parentWidth = Core.scene.getWidth();
        float parentHeight = Core.scene.getHeight();
        float winW = window.getWidth();
        float winH = window.getHeight();
        float minX = -winW * maxOut;
        float minY = -winH * maxOut;
        float maxX = parentWidth - winW * (1 - maxOut);
        float maxY = parentHeight - winH * (1 - maxOut);

        window.setPosition(Mathf.clamp(newX, minX, maxX), Mathf.clamp(newY, minY, maxY));
      }
    });

    titleTable.add(titleLabel).growX().fillX().left().padLeft(10f);

    Button closeBtn = new Button("@close", () -> remove());
    titleTable.add(closeBtn).align(Align.topRight);

    // --- 组装窗口 ---
    window.add(titleTable).growX(); 
    window.row();
    
    window.image().color(Color.valueOf("03ECEDFF")).fillX().height(1f);
    window.row();

    window.add(top).growX().top(); 
    window.row();
    window.add(scrollPane).grow().pad(10f);

    Core.scene.add(window);

    // 初始居中
    window.setPosition(
        (Core.scene.getWidth() - window.getWidth()) / 2,
        (Core.scene.getHeight() - window.getHeight()) / 2);
  }

  public void remove() {
    if (window != null) window.remove();
  }

  public void main(Table t) {}
  
  public void top(Table t) {}
public void low(Table t){
  
}
}