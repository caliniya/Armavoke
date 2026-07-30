package caliniya.armavoke.system.render;

import arc.Core;
import arc.Events;
import arc.graphics.Camera;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import caliniya.armavoke.base.shaders.SpaceShader;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.system.System;
import caliniya.armavoke.system.input.UniverseCameraInput;
import caliniya.armavoke.ui.fragment.UniverseFragment;
import caliniya.armavoke.world.stars.Universe;

/** 宇宙渲染 太空着色器背景 + 主/次网格线 + 交叉点圆点 + 选中高亮(填充+边框)。 */
public class UniverseRender extends System<UniverseRender> {

  /** 网格单元大小（像素） */
  public static final float GRID_SIZE = 256f;

  /** 主网格线颜色 */
  private static final Color MAJOR_COLOR = Color.white;

  /** 次要网格线颜色（半透明） */
  private static final Color MINOR_COLOR = new Color(1f, 1f, 1f, 0.35f);

  /** 主网格线粗细 */
  private static final float MAJOR_THICKNESS = 3f;

  /** 次要网格线粗细 */
  private static final float MINOR_THICKNESS = 6f;

  /** 选中高亮填充色 */
  private static final Color HIGHLIGHT_FILL = new Color(1f, 1f, 1f, 0.15f);

  /** 选中高亮边框色 */
  private static final Color HIGHLIGHT_OUTLINE = new Color(1f, 1f, 1f, 0.4f);

  /** 网格点颜色 */
  private static final Color DOT_COLOR = Color.white;

  /** 网格点半径 */
  private static final float DOT_RADIUS = 4f;

  /** 太空背景着色器 */
  private SpaceShader background;

  @Override
  public UniverseRender init() {
    this.index = 16;
    background = new SpaceShader();
    background.parallaxScale = 0.05f;
    background.baseScale = 0.6f;
    Events.run(EventType.events.EnterUV, () -> paused = false);
    Events.run(EventType.events.ExitUV, () -> paused = true);
    paused = true;
    return super.init(false,false);
  }

  @Override
  public void update() {
    if (paused) return;
    
    Camera cam = Render.universeCamera;
    float zoom = UniverseCameraInput.zoom;

    Draw.proj(cam);

    // 1. 太空背景
    background.render(cam, zoom);

    // 2. 网格线（主/次）
    drawGrid(cam);

    // 3. 交叉点圆点
    drawDots(cam, zoom);

    // 4. 选中高亮（填充 + 粗边框）
    if (Universe.hasSelection) {
      float cx = Universe.selectedX;
      float cy = Universe.selectedY;

      // 填充
      Draw.color(HIGHLIGHT_FILL);
      Fill.rect(cx + GRID_SIZE / 2f, cy + GRID_SIZE / 2f, GRID_SIZE, GRID_SIZE);

      // 粗边框（对应 Godot 的 draw_polyline 线条宽度*16）
      Draw.color(HIGHLIGHT_OUTLINE);
      Lines.stroke(MAJOR_THICKNESS * 4f);
      Lines.rect(cx, cy, GRID_SIZE, GRID_SIZE);

      Draw.color();
    }

    Draw.proj(Core.camera);
  }

  /** 绘制主/次网格线 */
  private void drawGrid(Camera cam) {
    float viewLeft = cam.position.x - cam.width / 2f;
    float viewBottom = cam.position.y - cam.height / 2f;
    float viewRight = cam.position.x + cam.width / 2f;
    float viewTop = cam.position.y + cam.height / 2f;

    float sx = (float) Math.floor(viewLeft / GRID_SIZE) * GRID_SIZE;
    float sy = (float) Math.floor(viewBottom / GRID_SIZE) * GRID_SIZE;
    float ex = (float) Math.ceil(viewRight / GRID_SIZE) * GRID_SIZE;
    float ey = (float) Math.ceil(viewTop / GRID_SIZE) * GRID_SIZE;

    // 垂直线
    int col = (int) (sx / GRID_SIZE);
    for (float x = sx; x <= ex; x += GRID_SIZE, col++) {
      Draw.color(MINOR_COLOR);
      Lines.stroke(MINOR_THICKNESS);
      Lines.line(x, sy, x, ey);
      Draw.color(MAJOR_COLOR);
      Lines.stroke(MAJOR_THICKNESS);
      Lines.line(x, sy, x, ey);
    }

    // 水平线
    int row = (int) (sy / GRID_SIZE);
    for (float y = sy; y <= ey; y += GRID_SIZE, row++) {
      Draw.color(MINOR_COLOR);
      Lines.stroke(MINOR_THICKNESS);
      Lines.line(sx, y, ex, y);
      Draw.color(MAJOR_COLOR);
      Lines.stroke(MAJOR_THICKNESS);
      Lines.line(sx, y, ex, y);
    }

    Draw.color();
  }

  /** 绘制交叉点圆点（对应 Godot 的 绘制网格点） */
  private void drawDots(Camera cam, float zoom) {
    // 缩太远不画点，避免性能问题
    if (zoom < 0.25f) return;

    float viewLeft = cam.position.x - cam.width / 2f;
    float viewBottom = cam.position.y - cam.height / 2f;
    float viewRight = cam.position.x + cam.width / 2f;
    float viewTop = cam.position.y + cam.height / 2f;

    float sx = (float) Math.floor(viewLeft / GRID_SIZE) * GRID_SIZE;
    float sy = (float) Math.floor(viewBottom / GRID_SIZE) * GRID_SIZE;
    float ex = (float) Math.ceil(viewRight / GRID_SIZE) * GRID_SIZE;
    float ey = (float) Math.ceil(viewTop / GRID_SIZE) * GRID_SIZE;

    Draw.color(DOT_COLOR);
    for (float x = sx; x <= ex; x += GRID_SIZE) {
      for (float y = sy; y <= ey; y += GRID_SIZE) {
        Fill.circle(x, y, DOT_RADIUS);
      }
    }
    Draw.color();
  }

  @Override
  public void dispose() {
    if (background != null) background.dispose();
    super.dispose();
  }
}
