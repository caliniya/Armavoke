package caliniya.armavoke.game.data;

import arc.func.Boolf;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Unit;

public class CommandData {
  // 包括UI需要的建筑和单位，资源相关数据。

  // 当前选中的单位
  public static Ar<Unit> checkedUnits = new Ar<Unit>();
    public static boolean commanding;

  // 初始化，也包括重置数据
  public void init() {
    checkedUnits.clear();
  }

  /** 根据像素坐标查找该位置上的单位（不过滤） */
  public Unit findUnitAt(float x, float y) {
    return findUnitAt(x, y, (Boolf<Unit>) null);
  }

  /** 根据像素坐标查找该位置上的单位（指定阵营） */
  public Unit findUnitAt(float x, float y, TeamTypes targetTeam) {
    return findUnitAt(x, y, u -> u.team == targetTeam);
  }

  /** 根据像素坐标查找该位置上的单位（排除阵营） */
  public Unit findUnitAtExclude(float x, float y, TeamTypes excludeTeam) {
    return findUnitAt(x, y, u -> u.team != excludeTeam);
  }

  /**
   * 核心查找方法：根据像素坐标和自定义过滤器查找单位
   *
   * @param x 世界坐标 X
   * @param y 世界坐标 Y
   * @param filter 过滤器接口，返回 true 表示符合条件，返回 false 表示跳过。传 null 表示不过滤。
   * @return 该位置上的单位，如果没有则返回 null
   */
  public Unit findUnitAt(float x, float y, Boolf<Unit> filter) {
    int gridW = WorldData.gridW;
    int gridH = WorldData.gridH;
    Ar<Unit>[] grid = WorldData.unitGrid;
    float chunkSize = WorldData.CHUNK_PIXEL_SIZE;

    int cx = (int) (x / chunkSize);
    int cy = (int) (y / chunkSize);

    // 边界检查
    if (cx < 0 || cx >= gridW || cy < 0 || cy >= gridH) {
      return null;
    }

    // 检测当前所在的区块及其周围 3x3 范围
    for (int dy = -1; dy <= 1; dy++) {
      int ncy = cy + dy;
      if (ncy < 0 || ncy >= gridH) continue;
      int rowOffset = ncy * gridW;

      for (int dx = -1; dx <= 1; dx++) {
        int ncx = cx + dx;
        if (ncx < 0 || ncx >= gridW) continue;

        Ar<Unit> units = grid[rowOffset + ncx];
        if (units == null || units.size == 0) continue;

        Object[] uItems = units.items;
        int uSize = units.size;

        for (int j = 0; j < uSize; j++) {
          Unit u = (Unit) uItems[j];
          if (u == null || u.health <= 0) continue;

          // 1. 自定义过滤器校验
          if (filter != null && !filter.get(u)) continue;

          // 2. 粗略检测：外接圆判定
          // 如果点不在外接圆内，则一定不在单位内部
          float halfSize = u.size / 2f;
          float diffX = x - u.x;
          float diffY = y - u.y;

          if (diffX * diffX + diffY * diffY > halfSize * halfSize) {
            continue;
          }

          // 3. 精确检测：使用单位自身的形状判定
          // 调用 Unit.contains 处理旋转、多方块等复杂逻辑
          if (u.contains(x, y)) {
            return u;
          }
        }
      }
    }

    return null;
  }
}
