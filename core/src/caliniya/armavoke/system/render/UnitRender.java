package caliniya.armavoke.system.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.type.BulletType;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;
import caliniya.armavoke.system.world.BulletProcess;
import caliniya.armavoke.ui.fragment.UniverseFragment;

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
    if (UniverseFragment.showing) return;
    // 绘制单位
    WorldData.units.each(
        u -> {
          if (shouldDraw(u.x, u.y, u.size * 2)) {
            u.draw();
            // 调用单位内部的调试绘制方法
            if (debug) {
              u.drawDebug();
            }
          }
        });

    // 绘制子弹
    // 用与 BulletProcess 相同的固定锁对象，确保与逻辑线程的缓冲交换互斥，
    // 避免拷到正在被清空/重填的缓冲导致子弹闪烁。
    temp.clear();
    synchronized (Systems.BP.BULLET_LOCK) {
      temp.addAll(WorldData.bullets);
    }
    temp.each(
        b -> {
          if (shouldDraw(b.x, b.y, b.type.size)) {
            b.type.draw(b);
          }
        });
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
}
