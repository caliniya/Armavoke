package caliniya.armavoke.system.input;

import arc.*;
import arc.input.*;
import arc.input.*;
import arc.math.geom.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.system.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.ui.fragment.*;
import caliniya.armavoke.world.stars.*;

/**
 * 宇宙视图网格选择器<br>
 * 实现 InputProcessor，通过多路复用器接收鼠标/触摸事件，更新 Universe 中的选中网格坐标。
 */
public class UniverseInput extends System<UniverseInput> implements InputProcessor {

  private final Vec2 world = new Vec2();

  @Override
  public UniverseInput init() {
    this.index = 2;
    return super.init(false);
  }

  /** 仅处理视图切换时的状态清理 */
  @Override
  public void update() {
    if (!UniverseFragment.showing) {
      Universe.hasSelection = false;
    }
  }

  /** 屏幕坐标 → 世界坐标 → 对齐网格 → 更新选中 */
  private void updateSelection(float screenX, float screenY) {
    if (!UniverseFragment.showing) {
      Universe.hasSelection = false;
      return;
    }

    world.set(screenX, screenY);
    Render.universeCamera.unproject(world);

    float gs = UniverseRender.GRID_SIZE;
    Universe.selectedX = (float) Math.floor(world.x / gs) * gs;
    Universe.selectedY = (float) Math.floor(world.y / gs) * gs;
    Universe.hasSelection = true;
  }

  // ====== InputProcessor 实现 ======

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    updateSelection(screenX, screenY);
    return false;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
    updateSelection(screenX, screenY);
    return false;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    updateSelection(screenX, screenY);
    return false;
  }

  // ====== 其余接口空实现 ======

  @Override public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button) { return false; }
  @Override public boolean keyDown(KeyCode key) { return false; }
  @Override public boolean keyUp(KeyCode key) { return false; }
  @Override public boolean keyTyped(char character) { return false; }
  @Override public boolean scrolled(float amountX, float amountY) { return false; }
}
