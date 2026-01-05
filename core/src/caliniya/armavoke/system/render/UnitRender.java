package caliniya.armavoke.system.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.geom.Point2;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;

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

    // 临时先写在这里
    for (Weapon weapon : u.weapons) {
      WeaponType w = weapon.type;
      TextureRegion reg = w.region;
      // 计算武器的世界位置
      // 假设 type.x 是左右偏移，type.y 是前后偏移
      // Angles.trnsx/y 计算旋转后的偏移量
      float wx = u.x + Angles.trnsx(u.rotation, w.x, w.y);
      float wy = u.y + Angles.trnsy(u.rotation, w.x, w.y);
      // 武器的绝对朝向 = 单位朝向 + 武器相对朝向
      float wRot = u.rotation + weapon.rotation;

      Draw.rect(reg, wx, wy, wRot);
    }
  }

  /** 绘制调试信息 */
  private void drawDebug(Unit u) {
    // 绘制碰撞体积
    Draw.color(Color.yellow);
    Lines.stroke(3f);
    Lines.rect(u.x - u.size / 2f, u.y - u.size / 2f, u.size, u.size);

    // 绘制目标点连接线 (从单位中心到目标点)
    // 仅当目标点不在原点或单位附近时绘制
    if (u.targetX != 0 || u.targetY != 0) {
      Draw.color(Color.orange);
      Lines.line(u.x, u.y, u.targetX, u.targetY);
      // 绘制目标点的一个小叉叉
      float s = 8f;
      Lines.line(u.targetX - s, u.targetY - s, u.targetX + s, u.targetY + s);
      Lines.line(u.targetX - s, u.targetY + s, u.targetX + s, u.targetY - s);
    }

    // 绘制寻路路径
    if (u.path != null && !u.path.isEmpty()) {
      Draw.color(Color.cyan);

      float lastX = u.x;
      float lastY = u.y;

      // 从当前路径索引开始绘制
      for (int i = u.pathIndex; i < u.path.size; i++) {
        Point2 p = u.path.get(i);
        // 转换网格坐标到世界中心坐标
        float wx = p.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
        float wy = p.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;

        // 绘制连线
        Lines.line(lastX, lastY, wx, wy);

        // 绘制节点小方块
        Fill.square(wx, wy, 3f);
        lastX = wx;
        lastY = wy;
      }
    }

    Draw.color(); // 重置颜色
  }
}
