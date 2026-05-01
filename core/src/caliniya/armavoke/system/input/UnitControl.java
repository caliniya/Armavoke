package caliniya.armavoke.system.input;

import arc.*;
import arc.input.GestureDetector.GestureListener;
import arc.input.KeyCode;
import arc.input.InputProcessor;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.game.data.CommandData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.*;

public class UnitControl implements InputProcessor, GestureListener {

  public UnitControl init() {
    // 状态现在由 CommandData.commanding 全局管理，无需本地监听器
    return this;
  }

  @Override
  public boolean tap(float x, float y, int count, KeyCode button) {
    if (!Core.app.isMobile()) return false;
    // 使用全局指挥状态判断
    if (!CommandData.commanding) return false;

    Vec2 worldPos = Core.camera.unproject(x, y);
    float wx = worldPos.x;
    float wy = worldPos.y;

    // 1. 尝试查找单位 (使用专用的选择判定逻辑)
    Unit target = findUnitForSelection(wx, wy);

    if (target != null) {
      if (target.team == Game.team) {
        toggleUnitSelection(target);
        return true;
      }
      // 如果点到了敌人，且当前有选中单位，可以视为攻击指令（此处暂略，执行移动逻辑）
    }

    // 2. 点击空地或敌人：尝试移动
    if (!CommandData.checkedUnits.isEmpty()) {
      issueMoveCommand(wx, wy);
      return true;
    }

    return false;
  }

  private void toggleUnitSelection(Unit u) {
    // 直接操作全局列表
    if (CommandData.checkedUnits.contains(u)) {
      u.isSelected = false;
      CommandData.checkedUnits.remove(u);
    } else {
      u.isSelected = true;
      CommandData.checkedUnits.add(u);
    }
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

        u.targetX = tx;
        u.targetY = ty;

        if (!WorldData.moveunits.contains(u)) {
          WorldData.moveunits.add(u);
        }
        u.pathed = false;
      }
    }
  }

  private boolean isSolidAtWorldPos(float wx, float wy) {
    int gx = (int) (wx / WorldData.TILE_SIZE);
    int gy = (int) (wy / WorldData.TILE_SIZE);
    return WorldData.world.isSolid(gx, gy);
  }

  /** 专门用于选择单位的查找逻辑。 将单位视作圆形，且直径为 size 的一半（半径为 size / 4）。 这样做是为了在点击复杂形状单位的边缘时，不会意外选中，体验更符合直觉。 */
  private Unit findUnitForSelection(float wx, float wy) {
    int cx = (int) (wx / WorldData.CHUNK_PIXEL_SIZE);
    int cy = (int) (wy / WorldData.CHUNK_PIXEL_SIZE);

    // 仅检查当前区块和周围 3x3 (简化版，足够覆盖选择半径)
    for (int dy = -1; dy <= 1; dy++) {
      int ncy = cy + dy;
      if (ncy < 0 || ncy >= WorldData.gridH) continue;
      int rowOffset = ncy * WorldData.gridW;

      for (int dx = -1; dx <= 1; dx++) {
        int ncx = cx + dx;
        if (ncx < 0 || ncx >= WorldData.gridW) continue;

        Ar<Unit> units = WorldData.unitGrid[rowOffset + ncx];
        if (units == null || units.isEmpty()) continue;

        // 倒序遍历，优先选中渲染在上层（列表靠后）的单位
        for (int i = units.size - 1; i >= 0; i--) {
          Unit u = units.get(i);
          if (u == null || u.health <= 0) continue;

          // 选择判定：半径 = size / 4.0f
          float radius = u.size / 4.0f;
          float dst2 = Mathf.dst2(wx, wy, u.x, u.y);

          if (dst2 <= radius * radius) {
            return u;
          }
        }
      }
    }
    return null;
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
    return false;
  }

  @Override
  public boolean panStop(float x, float y, int pointer, KeyCode button) {
    return false;
  }

  @Override
  public boolean zoom(float initialDistance, float distance) {
    return false;
  }

  @Override
  public boolean touchDown(float x, float y, int pointer, KeyCode button) {
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
