package caliniya.armavoke.system.input;

import arc.Core;
import arc.Events;
import arc.input.GestureDetector.GestureListener;
import arc.input.KeyCode;
import arc.input.InputProcessor;
import arc.math.geom.Vec2;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.*;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;

public class UnitControl implements InputProcessor, GestureListener {

  // 选中的单位列表
  public Ar<Unit> selectedUnits = new Ar<>(100);

  // 状态标志：是否处于指挥模式
  private boolean isCommandMode = false;

  private static final float MAX_UNIT_HALF_SIZE = 64f;

  public UnitControl init() {
    Events.on(
        EventType.CommandChange.class,
        event -> {
          this.isCommandMode = event.enabled;

          if (!this.isCommandMode) {
            clearSelection();
          }
        });

    return this;
  }

  @Override
  public boolean tap(float x, float y, int count, KeyCode button) {
    if (!Core.app.isMobile()) return false;
    if (!isCommandMode) return false;

    Vec2 worldPos = Core.camera.unproject(x, y);
    float wx = worldPos.x;
    float wy = worldPos.y;

    // 尝试查找单位
    Unit target = findUnitAt(wx, wy);

    if (target != null) {
      if (target.team == Game.team) {
        toggleUnitSelection(target);
        return true; 
      }
    } 
    
    // 点击空地(或者点击了敌人且未被拦截)：尝试移动
    if (!selectedUnits.isEmpty()) {
      issueMoveCommand(wx, wy);
      return true; 
    }
    

    return false;
  }

  private void toggleUnitSelection(Unit u) {
    if (selectedUnits.contains(u)) {
      u.isSelected = false;
      selectedUnits.remove(u);
    } else {
      u.isSelected = true;
      selectedUnits.add(u);
    }
  }

  /** 下达移动指令 */
  private void issueMoveCommand(float tx, float ty) {
    // 严格边界检查
    float mapWidth = WorldData.world.W * WorldData.TILE_SIZE;
    float mapHeight = WorldData.world.H * WorldData.TILE_SIZE;

    // 如果点击坐标在地图外，直接无视
    if (tx < 0 || ty < 0 || tx >= mapWidth || ty >= mapHeight) {
      return;
    }

    // 2. 障碍物检查
    if (isSolidAtWorldPos(tx, ty)) {
      return;
    }

    // 3. 执行指令
    synchronized (WorldData.moveunits) {
      for (int i = 0; i < selectedUnits.size; i++) {
        Unit u = selectedUnits.get(i);
        
        if (u == null || u.health <= 0) continue;
        
        u.targetX = tx;
        u.targetY = ty;

        // 加入移动处理列表
        if (!WorldData.moveunits.contains(u)) {
          WorldData.moveunits.add(u);
        }
        
        // 重置状态，触发 UnitMath 重新计算
        u.pathed = false; 
      }
    }
  }

  private boolean isSolidAtWorldPos(float wx, float wy) {
    int gx = (int) (wx / WorldData.TILE_SIZE);
    int gy = (int) (wy / WorldData.TILE_SIZE);
    return WorldData.world.isSolid(gx, gy);
  }

  private void clearSelection() {
    for (int i = 0; i < selectedUnits.size; i++) {
      selectedUnits.get(i).isSelected = false;
    }
    selectedUnits.clear();
  }


  private boolean isPointInUnit(Unit unit, float px, float py) {
    if (unit == null || unit.type == null) return false;
    // 使用你代码中提供的 size 字段
    float halfW = unit.size / 2f;
    float halfH = unit.size / 2f;
    return Math.abs(unit.x - px) <= halfW && Math.abs(unit.y - py) <= halfH;
  }

  private Unit findUnitAt(float wx, float wy) {
    int cx = (int) (wx / WorldData.CHUNK_PIXEL_SIZE);
    int cy = (int) (wy / WorldData.CHUNK_PIXEL_SIZE);

    Unit found = searchInChunk(cx, cy, wx, wy);
    if (found != null) return found;

    float localX = wx % WorldData.CHUNK_PIXEL_SIZE;
    float localY = wy % WorldData.CHUNK_PIXEL_SIZE;
    if (localX < 0) localX += WorldData.CHUNK_PIXEL_SIZE;
    if (localY < 0) localY += WorldData.CHUNK_PIXEL_SIZE;

    if (localX < MAX_UNIT_HALF_SIZE) found = searchInChunk(cx - 1, cy, wx, wy);
    else if (localX > WorldData.CHUNK_PIXEL_SIZE - MAX_UNIT_HALF_SIZE)
      found = searchInChunk(cx + 1, cy, wx, wy);
    if (found != null) return found;

    if (localY < MAX_UNIT_HALF_SIZE) found = searchInChunk(cx, cy - 1, wx, wy);
    else if (localY > WorldData.CHUNK_PIXEL_SIZE - MAX_UNIT_HALF_SIZE)
      found = searchInChunk(cx, cy + 1, wx, wy);

    return found;
  }

  private Unit searchInChunk(int cx, int cy, float wx, float wy) {
    if (cx < 0 || cx >= WorldData.gridW || cy < 0 || cy >= WorldData.gridH) return null;
    if (WorldData.unitGrid == null) return null;
    int index = cy * WorldData.gridW + cx;
    Ar<Unit> list = WorldData.unitGrid[index];
    if (list == null || list.isEmpty()) return null;

    for (int i = list.size - 1; i >= 0; i--) {
      Unit u = list.get(i);
      if (isPointInUnit(u, wx, wy)) return u;
    }
    return null;
  }

  // InputProcessor 接口的空实现...
  @Override public boolean touchDown(int x, int y, int p, KeyCode b) { return false; }
  @Override public boolean pinch(Vec2 i1, Vec2 i2, Vec2 p1, Vec2 p2) { return false; }
  @Override public boolean longPress(float x, float y) { return false; }
  @Override public boolean fling(float vx, float vy, KeyCode button) { return false; }
  @Override public boolean pan(float x, float y, float dx, float dy) { return false; }
  @Override public boolean panStop(float x, float y, int pointer, KeyCode button) { return false; }
  @Override public boolean zoom(float initialDistance, float distance) { return false; }
  @Override public boolean touchDown(float x, float y, int pointer, KeyCode button) { return false; }
  @Override public boolean keyDown(KeyCode key) { return false; }
  @Override public boolean keyUp(KeyCode key) { return false; }
  @Override public boolean keyTyped(char character) { return false; }
  @Override public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button) { return false; }
  @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
  @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
  @Override public boolean scrolled(float amountX, float amountY) { return false; }
}