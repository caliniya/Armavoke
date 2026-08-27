package caliniya.vergvoke.ui.windows;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;

/**
 * 进度窗口。
 *
 * <p>提供一个可被外部修改的进度参数 {@link #setProgress(float)}，并在窗口中显示一条进度条 + 百分比文字。
 *
 * <h3>用法</h3>
 *
 * <pre>{@code
 * // 1. 创建并显示（build 必须在渲染/主线程）
 * ProgressWindow pw = new ProgressWindow("加载中", "正在生成地图...");
 * pw.build();
 *
 * // 2. 任意线程（含后台加载线程）安全地更新进度
 * pw.setProgress(0.35f);     // 35%
 * pw.setDescription("正在放置建筑...");
 *
 * // 3. 完成后关闭（remove 需在渲染/主线程）
 * pw.setProgress(1f);
 * pw.remove();
 * }</pre>
 *
 * <p><b>线程说明：</b>{@link #setProgress(float)} 只写一个 {@code volatile} 字段，可在任意线程调用；
 * 而 {@link #build()} / {@link #remove()} / {@link #setDescription(String)} 会操作 scene 图，
 * 必须在渲染/主线程调用。
 */
public class ProgressWindow extends Window {

  /** 进度值，范围 [0,1]。volatile 保证后台线程写入后渲染线程立即可见。 */
  private volatile float progress = 0f;

  /** 进度条上方的描述文字。 */
  private String description;

  /** 进度条轨道（背景）颜色。 */
  public Color trackColor = Color.valueOf("22242AFF");

  /** 进度条填充（已完成部分）颜色。 */
  public Color fillColor = Color.valueOf("03ECEDFF");

  /** 进度条高度（宽度随窗口自适应）。 */
  public float barHeight = 22f;

  private Label percentLabel;
  private Label descLabel;

  public ProgressWindow() {
    this("进度", "");
  }

  public ProgressWindow(String title) {
    this(title, "");
  }

  public ProgressWindow(String title, String description) {
    super(title);
    this.description = description == null ? "" : description;
    this.w = 440f;
    this.h = 190f;
    showFullButton = false; // 进度提示弹窗不需要全屏
  }

  /**
   * 便捷创建：new + build 一步到位（必须在渲染/主线程调用）。
   *
   * @return 已显示的窗口实例，可继续用于 setProgress
   */
  public static ProgressWindow show(String title, String description) {
    ProgressWindow w = new ProgressWindow(title, description);
    w.build();
    return w;
  }

  // ==================== 对外接口 ====================

  /** 设置进度，自动裁剪到 [0,1]。线程安全，可在任意线程调用。 */
  public void setProgress(float value) {
    this.progress = Mathf.clamp(value);
  }

  /** 在当前进度基础上增加一个增量（同样裁剪到 [0,1]）。线程安全。 */
  public void addProgress(float delta) {
    this.progress = Mathf.clamp(this.progress + delta);
  }

  /** 读取当前进度 [0,1]。 */
  public float getProgress() {
    return progress;
  }

  /** 设置描述文字（需在渲染/主线程调用）。 */
  public void setDescription(String text) {
    this.description = text == null ? "" : text;
    if (descLabel != null) descLabel.setText(this.description);
  }

  // ==================== 内容构建 ====================

  @Override
  public void main(Table t) {
    t.clear();
    t.defaults().pad(6f);

    // 描述文字
    descLabel = new Label(description);
    descLabel.setColor(Color.white);
    t.add(descLabel).growX().left();
    t.row();

    // 进度条（自绘 Element）
    t.add(new ProgressBar()).growX().height(barHeight).padTop(8f).padBottom(4f);
    t.row();

    // 百分比文字：每帧根据 progress 自动刷新
    percentLabel = new Label("0%");
    percentLabel.setColor(fillColor);
    percentLabel.update(() -> percentLabel.setText((int) (Mathf.clamp(progress) * 100) + "%"));
    t.add(percentLabel).right();
  }

  /** 自绘进度条：一条暗色轨道 + 一条按 progress 铺开的亮色填充。 */
  private class ProgressBar extends Element {
    @Override
    public void draw() {
      float p = Mathf.clamp(progress);
      Drawable white = Core.atlas.getDrawable("white");

      float bx = x + translation.x;
      float by = y + translation.y;

      // 轨道（背景）
      Draw.color(trackColor);
      Draw.alpha(trackColor.a * parentAlpha);
      white.draw(bx, by, width, height);

      // 填充（已完成部分）
      if (p > 0f) {
        Draw.color(fillColor);
        Draw.alpha(fillColor.a * parentAlpha);
        white.draw(bx, by, width * p, height);
      }

      Draw.reset();
    }
  }
}
