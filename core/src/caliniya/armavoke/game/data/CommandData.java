package caliniya.armavoke.game.data;

import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Unit;

public class CommandData {
  // 包括UI需要的建筑和单位，资源相关数据。

  // 当前选中的单位
  public static Ar<Unit> checkedUnits = new Ar<Unit>();

  // 初始化，也包括重置数据
  public void init() {
    checkedUnits.clear();
  }

  /**
   * 根据像素坐标查找该位置上的单位（点检测）
   *
   * @param x 世界坐标 X
   * @param y 世界坐标 Y
   * @return 该位置上的单位，如果没有则返回 null
   */
  public Unit findUnitAt(float x, float y) {
    return findUnitAt(x, y, null, false);
  }

  /**
   * 根据像素坐标查找该位置上的单位（点检测）
   *
   * @param x 世界坐标 X
   * @param y 世界坐标 Y
   * @param targetTeam 指定阵营（只检测该阵营），为 null 时检测所有阵营
   * @return 该位置上的单位，如果没有则返回 null
   */
  public Unit findUnitAt(float x, float y, TeamTypes targetTeam) {
    return findUnitAt(x, y, targetTeam, true);
  }

  /**
   * 根据像素坐标查找该位置上的单位（点检测）
   *
   * @param x 世界坐标 X
   * @param y 世界坐标 Y
   * @param excludeTeam 需要排除的阵营（不检测该阵营），为 null 时不排除任何阵营
   * @return 该位置上的单位，如果没有则返回 null
   */
  public Unit findUnitAtExclude(float x, float y, TeamTypes excludeTeam) {
    return findUnitAt(x, y, excludeTeam, false);
  }

  /**
   * 内部实现：根据像素坐标查找单位
   *
   * @param x 世界坐标 X
   * @param y 世界坐标 Y
   * @param teamFilter 阵营过滤器（null 表示不过滤）
   * @param matchTarget 为 true 时只检测 teamFilter 指定的阵营，为 false 时排除 teamFilter 指定的阵营
   * @return 该位置上的单位，如果没有则返回 null
   */
  private Unit findUnitAt(float x, float y, TeamTypes teamFilter, boolean matchTarget) {
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

    // 检测当前所在的区块及其周围 3x3 范围（考虑到单位可能跨区块）
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

          // 阵营过滤
          if (teamFilter != null) {
            boolean sameTeam = u.team == teamFilter;
            if (matchTarget ? !sameTeam : sameTeam) continue;
          }

          // 点碰撞检测：检查给定坐标是否在单位的 AABB 范围内
          float halfSize = u.size / 2f;
          if (x >= u.x - halfSize
              && x <= u.x + halfSize
              && y >= u.y - halfSize
              && y <= u.y + halfSize) {
            return u;
          }
        }
      }
    }

    return null;
  }
}
