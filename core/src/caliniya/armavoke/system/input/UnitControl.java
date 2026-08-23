package caliniya.armavoke.system.input;

import arc.Core;
import arc.input.GestureDetector.GestureListener;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.ecs.runtime.EcsUnitRuntime;
import caliniya.armavoke.game.data.CommandData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.ui.windows.FactoryMenuWindow;
import caliniya.armavoke.world.blocks.produce.unit.Factory;

/** Input commands applied directly to ECS units and buildings. */
public class UnitControl implements InputProcessor, GestureListener {
  private static final float selectRadius = 100f;
  private static final float dragThreshold = 8f;
  private float downX, downY;
  private boolean dragged;

  public UnitControl init() { return this; }

  private Vec2 world(float screenX, float screenY) {
    return Core.camera.unproject(new Vec2(screenX, screenY));
  }

  @Override
  public boolean tap(float x, float y, int count, KeyCode button) {
    Vec2 point = world(x, y);
    if (!CommandData.commanding) return openFactory(point.x, point.y);
    if (button == KeyCode.mouseRight || CommandData.commandType == CommandData.CommandType.Move) {
      for (Unit unit : CommandData.checkedUnits) EcsUnitRuntime.commandMove(unit, point.x, point.y);
      return !CommandData.checkedUnits.isEmpty();
    }
    Unit unit = CommandData.findUnitAt(point.x, point.y, selectRadius);
    CommandData.clearSelection();
    return CommandData.select(unit);
  }

  private boolean openFactory(float x, float y) {
    if (WorldData.world == null) return false;
    Building building = WorldData.world.getBuilding((int) (x / WorldData.TILE_SIZE), (int) (y / WorldData.TILE_SIZE));
    if (building == null || !(building.block() instanceof Factory)) return false;
    new FactoryMenuWindow(building).build();
    return true;
  }

  @Override public boolean touchDown(float x, float y, int pointer, KeyCode button) {
    downX = x; downY = y; dragged = false; return false;
  }
  @Override public boolean pan(float x, float y, float dx, float dy) {
    if (Math.abs(x - downX) + Math.abs(y - downY) > dragThreshold) dragged = true;
    if (CommandData.boxSelect) {
      Vec2 start = world(downX, downY), end = world(x, y);
      CommandData.boxDragging = true;
      CommandData.boxStartX = start.x; CommandData.boxStartY = start.y;
      CommandData.boxEndX = end.x; CommandData.boxEndY = end.y;
    } else {
      Core.camera.position.x -= dx * Render.currentZoom;
      Core.camera.position.y += dy * Render.currentZoom;
    }
    return true;
  }
  @Override public boolean panStop(float x, float y, int pointer, KeyCode button) {
    if (!CommandData.boxDragging) return false;
    float minX = Math.min(CommandData.boxStartX, CommandData.boxEndX);
    float minY = Math.min(CommandData.boxStartY, CommandData.boxEndY);
    float maxX = Math.max(CommandData.boxStartX, CommandData.boxEndX);
    float maxY = Math.max(CommandData.boxStartY, CommandData.boxEndY);
    Ar<Unit> selected = new Ar<>();
    EcsQueries.intersectUnits(minX, minY, maxX - minX, maxY - minY, selected::add);
    CommandData.replaceSelection(selected);
    CommandData.boxDragging = false;
    return true;
  }
  @Override public boolean zoom(float initialDistance, float distance) {
    if (distance != 0f) {
      float target = Math.max(0.1f, Math.min(8f, Render.currentZoom * initialDistance / distance));
      Render.zoom(target - Render.currentZoom);
    }
    return true;
  }
  @Override public boolean keyDown(KeyCode key) {
    if (key == KeyCode.escape) CommandData.clearSelection();
    return false;
  }
  @Override public boolean touchDown(int x, int y, int pointer, KeyCode button) { return touchDown((float) x, y, pointer, button); }
  @Override public boolean pinch(Vec2 i1, Vec2 i2, Vec2 p1, Vec2 p2) { return false; }
  @Override public boolean longPress(float x, float y) { return false; }
  @Override public boolean fling(float vx, float vy, KeyCode button) { return false; }
  @Override public boolean keyUp(KeyCode key) { return false; }
  @Override public boolean keyTyped(char character) { return false; }
  @Override public boolean touchUp(int x, int y, int pointer, KeyCode button) { return false; }
  @Override public boolean touchDragged(int x, int y, int pointer) { return false; }
  @Override public boolean mouseMoved(int x, int y) { return false; }
  @Override public boolean scrolled(float x, float y) { return false; }
}
