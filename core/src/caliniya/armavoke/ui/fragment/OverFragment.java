package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.scene.event.Touchable;
import arc.util.Disposable;
import caliniya.armavoke.base.shader.BlurShader;
import caliniya.armavoke.core.Render;

/** 覆盖型 UI 基类 自动处理背景截图、毛玻璃模糊效果以及资源释放 */
public abstract class OverFragment extends Table implements Disposable {

  private static BlurShader blurShader;
  private FrameBuffer snapshotFbo;

  // 子类将内容添加到这个容器中
  protected Table mainTable;

  public OverFragment() {
    // 1. 懒加载 Shader (全局单例)
    if (blurShader == null) {
      blurShader = new BlurShader();
    }

    // 2. 捕获当前游戏画面
    captureSnapshot();

    // 3. 初始化基础 UI 属性
    setupBaseUI();
  }

  /** 截图逻辑 */
  private void captureSnapshot() {
    int w = Core.graphics.getWidth();
    int h = Core.graphics.getHeight();

    // 创建帧缓冲
    snapshotFbo = new FrameBuffer(w, h);

    snapshotFbo.begin(Color.clear);

    Render.updateAll();

    Draw.flush();
    snapshotFbo.end();
  }

  private void setupBaseUI() {
    this.setFillParent(true);
    this.touchable = Touchable.enabled; // 拦截点击

    // 设置半透明黑底 (叠加在模糊图之上)
    this.background(Core.atlas.drawable("white")).setColor(0, 0, 0, 0.4f);

    // 创建居中的主窗口容器
    mainTable = new Table();
    // 默认窗口样式 (深灰背景)
    mainTable.background(Core.atlas.drawable("white")).setColor(0.2f, 0.2f, 0.2f, 1f);

    // 让子类填充内容
    buildContent(mainTable);

    // 将窗口添加到屏幕中间，四周留出 60px 边距
    this.add(mainTable).grow().margin(60f);
  }

  /** 子类必须实现此方法来构建具体内容 */
  protected abstract void buildContent(Table container);

  @Override
  public void draw() {
    // 1. 绘制模糊背景
    if (snapshotFbo != null) {
      Draw.flush();
      Draw.shader(blurShader);

      TextureRegion tex = Draw.wrap(snapshotFbo.getTexture());
      Draw.color(Color.white);

      // 绘制全屏 (注意 Y 轴翻转)
      Draw.rect(
          tex,
          Core.graphics.getWidth() / 2f,
          Core.graphics.getHeight() / 2f,
          Core.graphics.getWidth(),
          -Core.graphics.getHeight());

      Draw.flush();
      Draw.shader();
    }

    // 2. 绘制自身 (黑底 + 窗口内容)
    super.draw();
  }

  /** 关闭并销毁资源 */
  public void close() {
    this.remove(); // 从场景移除
    dispose(); // 释放 FBO
  }

  @Override
  public void dispose() {
    if (snapshotFbo != null) {
      snapshotFbo.dispose();
      snapshotFbo = null;
    }
  }
  
  public void build() {
    Core.scene.root.addChild(this);
  }
}
