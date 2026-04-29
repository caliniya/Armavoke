package caliniya.armavoke.system.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.type.BulletType;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;

public class UnitRender extends System<UnitRender> {

  // 调试开关
  public static boolean debug = true;

  public static Ar<Bullet> temp = new Ar<Bullet>(false, 1000);

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
        u.draw();
        // 调用单位内部的调试绘制方法
        if (debug) {
          u.drawDebug();
        }
      }
    }

    // 绘制子弹
    temp.clear();
    synchronized (WorldData.bullets) {
      temp.addAll(WorldData.bullets);
    }

    for (int i = 0; i < temp.size; ++i) {
      Bullet b = temp.get(i);
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

  // 移除了旧的 drawDebug(Unit u) 方法
}
