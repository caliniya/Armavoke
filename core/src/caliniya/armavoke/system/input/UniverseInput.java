package caliniya.armavoke.system.input;

import arc.Core;
import arc.Events;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.world.stars.StarNode;
import caliniya.armavoke.world.stars.Universe;

/** 星图节点交互：点击选择、空白取消、鼠标悬停高亮。 */
public class UniverseInput implements InputProcessor {

  private final Vec2 world = new Vec2();
  private static final float queryRadius = 128f;
  public boolean paused;

  public UniverseInput() {
    Events.run(
        EventType.events.EnterUV,
        () -> {
          paused = false;
          Universe.clearSelection();
        });
    Events.run(
        EventType.events.ExitUV,
        () -> {
          paused = true;
          Universe.clearSelection();
        });
    paused = true;
  }

  private StarNode nodeAt(float screenX, float screenY) {
    if (paused || Game.starMap == null) return null;
    world.set(screenX, screenY);
    Render.universeCamera.unproject(world);

    final StarNode[] result = {null};
    final float[] nearest = {Float.MAX_VALUE};
    Game.starMap.getNode(
        world.x - queryRadius,
        world.y - queryRadius,
        queryRadius * 2f,
        queryRadius * 2f,
        node -> {
          float dst2 = (node.x - world.x) * (node.x - world.x)
              + (node.y - world.y) * (node.y - world.y);
          float radius = node.size / 2f;
          if (dst2 <= radius * radius && dst2 < nearest[0]) {
            nearest[0] = dst2;
            result[0] = node;
          }
        });
    return result[0];
  }

  // ====== InputProcessor 实现 ======

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    if (!paused) Universe.hoverNode = nodeAt(screenX, screenY);
    return false;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
    if (paused) return false;
    Universe.selectedNode = nodeAt(screenX, screenY);
    UI.universe.showNode(Universe.selectedNode);
    return Universe.selectedNode != null;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    if (!paused) Universe.hoverNode = nodeAt(screenX, screenY);
    return false;
  }

  // ====== 其余接口空实现 ======

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button) {
    return false;
  }

  @Override
  public boolean keyDown(KeyCode key) {
    return false;
  }

  @Override
  public boolean keyUp(KeyCode key) {
    return false;
  }

  @Override
  public boolean keyTyped(char character) {
    return false;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    return false;
  }
}
