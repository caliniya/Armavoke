package caliniya.armavoke.system.input;

import arc.Core;
import arc.math.geom.Vec2;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.system.System;
import caliniya.armavoke.system.render.UniverseRender;
import caliniya.armavoke.ui.fragment.UniverseFragment;
import caliniya.armavoke.world.stars.Universe;

/**
 * 宇宙视图网格选择器<br>
 * 检测鼠标/触摸位置，更新 Universe 中的选中网格坐标。 todo 需要重做，加入多路终端复用器
 */
public class UniverseInput extends System<UniverseInput> {

  private final Vec2 world = new Vec2();

  @Override
  public UniverseInput init() {
    this.index = 2;
    return super.init(false);
  }

  @Override
  public void update() {
    if (!UniverseFragment.showing) {
      Universe.hasSelection = false;
      return;
    }

    // 屏幕坐标 → 世界坐标
    world.set(Core.input.mouseX(), Core.input.mouseY());
    Render.universeCamera.unproject(world);

    // 对齐到网格
    float gs = UniverseRender.GRID_SIZE;
    Universe.selectedX = (float) Math.floor(world.x / gs) * gs;
    Universe.selectedY = (float) Math.floor(world.y / gs) * gs;
    Universe.hasSelection = true;
  }
}
