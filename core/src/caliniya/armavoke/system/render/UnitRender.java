package caliniya.armavoke.system.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.geom.Point2;
import arc.util.Align;
import arc.util.Strings;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;
import caliniya.armavoke.ui.Fonts;

public class UnitRender extends BasicSystem<UnitRender> {

  // 调试开关
  public static boolean debug = true;

  @Override
  public UnitRender init() {
    this.index = 7;
    return super.init();
  }

  @Override
  public void update() {
    // 绘制单位
    for (int i = 0; i < WorldData.units.size; i++) {
      Unit u = WorldData.units.get(i);
      if (shouldDraw(u.x, u.y, u.size * 2)) {
        drawUnit(u);
        if (debug) drawDebug(u);
      }
    }

    for (int i = 0; i < WorldData.bullets.size; i++) {
      Bullet b = WorldData.bullets.get(i);
      if (shouldDraw(b.x, b.y, 64f)) {
        drawBullet(b);
      }
    }
  }

  // 通用的剔除方法
  private boolean shouldDraw(float x, float y, float size) {
    float viewX = Core.camera.position.x;
    float viewY = Core.camera.position.y;
    float buffer = debug ? 500f : size;
    float w = Core.camera.width / 2f + buffer;
    float h = Core.camera.height / 2f + buffer;
    return x > viewX - w && x < viewX + w && y > viewY - h && y < viewY + h;
  }

  // 子弹绘制逻辑
  private void drawBullet(Bullet b) {
    if (b.type == null) return;
    b.type.draw(b);
  }

  private void drawUnit(Unit u) {
    // 选中状态圈
    if (u.isSelected) {
      Draw.color(Color.green);
      Lines.stroke(2f);
      Lines.circle(u.x, u.y, u.size + 4);
      Draw.color();
    }

    Draw.rect(u.region, u.x, u.y, u.rotation);
    Draw.rect(u.cell, u.x, u.y, u.rotation);

    // 绘制武器
    for (Weapon weapon : u.weapons) {
      float wRot = u.rotation + weapon.rotation;
      Draw.rect(weapon.type.region, weapon.wx, weapon.wy, wRot);
    }
  }

  /** 绘制调试信息 */
  private void drawDebug(Unit u) {
    // 1. 绘制碰撞体积 (黄色)
    Draw.color(Color.yellow);
    Lines.stroke(3f);
    Lines.rect(u.x - u.size / 2f, u.y - u.size / 2f, u.size, u.size);

    // 2. 绘制速度向量 (洋红色)
    // 只有当速度大于微小阈值时才绘制，避免视觉干扰
    if (Math.abs(u.speedX) > 0.001f || Math.abs(u.speedY) > 0.001f) {
        Draw.color(Color.magenta);
        // 放大倍数，因为每帧移动的像素很少，放大后才能看清方向
        float scale = 20f; 
        Lines.line(u.x, u.y, u.x + u.speedX * scale, u.y + u.speedY * scale);
        
        // 绘制具体数值文本
        // 使用 arc.graphics.g2d.Fonts.def 或者你自己定义的 Fonts
        // 注意：字体绘制比较消耗性能，仅在Debug模式使用
        Fonts.def.draw(
            Strings.format(u.speedX + " " + u.speedY), 
            u.x, 
            u.y + u.size + 8f, // 显示在单位上方
            Align.center
        );
    }

    // 3. 绘制目标点连接线 (橙色)
    if (u.targetX != 0 || u.targetY != 0) {
      Draw.color(Color.orange);
      Lines.line(u.x, u.y, u.targetX, u.targetY);
      float s = 8f;
      Lines.line(u.targetX - s, u.targetY - s, u.targetX + s, u.targetY + s);
      Lines.line(u.targetX - s, u.targetY + s, u.targetX + s, u.targetY - s);
    }

    // 4. 绘制寻路路径 (青色)
    if (u.path != null && !u.path.isEmpty()) {
      Draw.color(Color.cyan);

      float lastX = u.x;
      float lastY = u.y;

      for (int i = u.pathIndex; i < u.path.size; i++) {
        Point2 p = u.path.get(i);
        float wx = p.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
        float wy = p.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;

        Lines.line(lastX, lastY, wx, wy);
        Fill.square(wx, wy, 3f);
        lastX = wx;
        lastY = wy;
      }
    }

    Draw.color(); // 重置颜色
  }
}