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

import javax.swing.JFrame;

public class Window {

  public Table window;
  public Table main, top, low;
  public float w = 400f, h = 300f;
  public float maxOut = 0.8f; // 允许80%移出屏幕
  public String title = "Window";
  
  public javax.swing.JFrame j;

  /** 是否为模态窗口。模态窗口会显示暗色遮罩并阻断背景所有输入。 */
  public boolean modal = false;

  /** 模态遮罩层，仅 modal=true 时使用 */
  protected Table modalOverlay;

  /** 遮罩透明度，0=完全透明，1=完全不透明。默认 0.5 */
  public float modalAlpha = 0.5f;

  public Window() {
    this("");
  }

  public Window(String titleText) {
    this.title = titleText;
  }

  /** 链式设置模态 */
  public Window modal(boolean modal) {
    this.modal = modal;
    return this;
  }

  /** 链式设置遮罩透明度 */
  public Window modalAlpha(float alpha) {
    this.modalAlpha = alpha;
    return this;
  }

  public void build() {
    if (window != null) window.remove();
    if (modalOverlay != null) modalOverlay.remove();

    window = new Table();
    window.setSize(w, h);

    window.setBackground(
        new NinePatchDrawable((NinePatchDrawable) Core.atlas.getDrawable("Window")));

    window.touchable = Touchable.enabled;
    window.cullable = true;

    // --- 模态遮罩 ---
    if (modal) {
      modalOverlay = new Table();
      modalOverlay.setFillParent(true);
      modalOverlay.touchable = Touchable.enabled;

      // 暗色半透明背景
      modalOverlay.setBackground(Core.atlas.getDrawable("white"));
      modalOverlay.setColor(0, 0, 0, modalAlpha);

      // 拦截所有点击，阻止穿透到背景
      modalOverlay.addListener(
          new InputListener() {
            @Override
            public boolean touchDown(
                InputEvent event, float x, float y, int pointer, arc.input.KeyCode button) {
              return true;
            }
          });

      Core.scene.add(modalOverlay);
    }

    // 容器
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
    titleLabel.addListener(
        new InputListener() {
          float grabX, grabY;

          @Override
          public boolean touchDown(
              InputEvent event, float x, float y, int pointer, arc.input.KeyCode button) {
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
    if (modalOverlay != null) {
      modalOverlay.remove();
      modalOverlay = null;
    }
  }

  public void main(Table t) {}

  public void top(Table t) {}

  public void low(Table t) {}
}
