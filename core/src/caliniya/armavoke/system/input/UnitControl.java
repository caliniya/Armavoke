package caliniya.armavoke.system.input;

import arc.*;
import arc.input.GestureDetector.GestureListener;
import arc.input.KeyCode;
import arc.input.InputProcessor;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.game.data.CommandData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.*;
import caliniya.armavoke.type.ai.UnitAI;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.ui.windows.FactoryMenuWindow;
import caliniya.armavoke.world.blocks.produce.unit.FactoryBuild;

public class UnitControl implements InputProcessor, GestureListener {

  private static final float selectRadius = 100f;
  private static final float dragThreshold = 8f;
  private float downX, downY;
  private boolean dragged;

  public UnitControl init() {
    return this;
  }

  @Override
  public boolean tap(float x, float y, int count, KeyCode button) {
    if (!CommandData.commanding) return openFactory(x, y);
    if (dragged) {
      dragged = false;
      return true;
    }

    Vec2 worldPos = Core.camera.unproject(x, y);
    float wx = worldPos.x;
    float wy = worldPos.y;

    Unit clicked = CommandData.findUnitAt(wx, wy, selectRadius);
    if (clicked != null && clicked.team == Game.team) {
      CommandData.select(clicked);
      UI.hud.refreshCommand();
      return true;
    }

    if (clicked != null && clicked.team != Game.team && !CommandData.checkedUnits.isEmpty()) {
      issueAttackCommand(clicked);
      return true;
    }

    // 按当前指挥状态执行（直接指挥）
    if (CommandData.commandType == CommandData.CommandType.Move) {
      if (!CommandData.checkedUnits.isEmpty()) {
        issueMoveCommand(wx, wy);
      }
      return true;
    } else if (CommandData.commandType == CommandData.CommandType.Stop) {
      stopUnits();
      return true;
    }

    return false;
  }

  /** 让选中单位立即停下（清目标/速度/寻路）。 */
  private void stopUnits() {
    for (Unit u : CommandData.checkedUnits) {
      if (u != null && u.ai != null) u.ai.stop();
    }
  }

  private boolean openFactory(float screenX, float screenY) {
    if (WorldData.world == null) return false;
    Vec2 worldPos = Core.camera.unproject(screenX, screenY);
    int tileX = (int) (worldPos.x / WorldData.TILE_SIZE);
    int tileY = (int) (worldPos.y / WorldData.TILE_SIZE);
    Building building = WorldData.world.getBuilding(tileX, tileY);
    if (building instanceof FactoryBuild factory && building.team == Game.team) {
      new FactoryMenuWindow(factory).build();
      return true;
    }
    return false;
  }

  /** 下达移动指令 */
  private void issueMoveCommand(float tx, float ty) {
    float mapWidth = WorldData.world.W * WorldData.TILE_SIZE;
    float mapHeight = WorldData.world.H * WorldData.TILE_SIZE;

    if (tx < 0 || ty < 0 || tx >= mapWidth || ty >= mapHeight) return;
    if (isSolidAtWorldPos(tx, ty)) return;

    synchronized (WorldData.moveunits) {
      for (int i = 0; i < CommandData.checkedUnits.size; i++) {
        Unit u = CommandData.checkedUnits.get(i);
        if (u == null || u.health <= 0) continue;

        if (u.ai != null) u.ai.moveTo(tx, ty);

        if (!WorldData.moveunits.array.contains(u)) {
          WorldData.moveunits.add(u);
        }
      }
    }
  }

  private void issueAttackCommand(Unit enemy) {
    for (Unit unit : CommandData.checkedUnits) {
      if (unit != null && unit.health > 0f && unit.ai != null) {
        unit.ai.attack(enemy);
      }
    }
  }

  private void finishBoxSelection() {
    Vec2 first =
        Core.camera.unproject(new Vec2(CommandData.boxStartX, CommandData.boxStartY));
    Vec2 second = Core.camera.unproject(new Vec2(CommandData.boxEndX, CommandData.boxEndY));
    float minX = Math.min(first.x, second.x);
    float minY = Math.min(first.y, second.y);
    float maxX = Math.max(first.x, second.x);
    float maxY = Math.max(first.y, second.y);

    Ar<Unit> selected = new Ar<>();
    WorldData.units.intersect(
        minX,
        minY,
        maxX - minX,
        maxY - minY,
        unit -> {
          if (unit != null && unit.health > 0f && unit.team == Game.team) selected.add(unit);
        });
    CommandData.replaceSelection(selected);
    UI.hud.refreshCommand();
  }

  private boolean isSolidAtWorldPos(float wx, float wy) {
    int gx = (int) (wx / WorldData.TILE_SIZE);
    int gy = (int) (wy / WorldData.TILE_SIZE);
    return WorldData.world.isSolid(gx, gy);
  }

  // InputProcessor 接口的空实现...
  @Override
  public boolean touchDown(int x, int y, int p, KeyCode b) {
    return false;
  }

  @Override
  public boolean pinch(Vec2 i1, Vec2 i2, Vec2 p1, Vec2 p2) {
    return false;
  }

  @Override
  public boolean longPress(float x, float y) {
    return false;
  }

  @Override
  public boolean fling(float vx, float vy, KeyCode button) {
    return false;
  }

  @Override
  public boolean pan(float x, float y, float dx, float dy) {
    if (!CommandData.commanding || !CommandData.boxSelect) return false;
    if (!CommandData.boxDragging) {
      CommandData.boxDragging = true;
      CommandData.boxStartX = downX;
      CommandData.boxStartY = downY;
    }
    CommandData.boxEndX = x;
    CommandData.boxEndY = y;
    dragged =
        Mathf.dst(CommandData.boxStartX, CommandData.boxStartY, x, y) >= dragThreshold;
    return true;
  }

  @Override
  public boolean panStop(float x, float y, int pointer, KeyCode button) {
    if (!CommandData.boxDragging) return false;
    CommandData.boxEndX = x;
    CommandData.boxEndY = y;
    if (dragged) finishBoxSelection();
    CommandData.boxDragging = false;
    return true;
  }

  @Override
  public boolean zoom(float initialDistance, float distance) {
    return false;
  }

  @Override
  public boolean touchDown(float x, float y, int pointer, KeyCode button) {
    if (!CommandData.commanding || !CommandData.boxSelect) return false;
    downX = x;
    downY = y;
    dragged = false;
    CommandData.boxStartX = x;
    CommandData.boxStartY = y;
    CommandData.boxEndX = x;
    CommandData.boxEndY = y;
    return true;
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
  public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button) {
    return false;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    return false;
  }

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    return false;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    return false;
  }
}
