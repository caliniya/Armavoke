package caliniya.armavoke.system.world;

import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.TeamData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;

// 单位移动控制和单位物理处理
// 在后台运行 (super.init(true))
public class UnitProces extends BasicSystem<UnitProces> {

  @Override
  public UnitProces init() {
    // 尝试以60的帧率更新
    return super.init(true);
  }

  @Override
  public void update() {
    
    Ar<Unit> list = WorldData.units;

    for (int i = 0; i < list.size; i++) {
      Unit u = list.get(i);

      if (u == null || u.health <= 0) continue;

      float oldX = u.x;
      float oldY = u.y;

      if (!(u.path == null) && !u.path.isEmpty()) {
        float distToTarget = Mathf.dst(u.x, u.y, u.targetX, u.targetY);
        if (distToTarget <= u.speed * this.delta) {
          // 强制吸附到目标点
          u.x = u.targetX;
          u.y = u.targetY;
          u.speedX = 0;
          u.speedY = 0;

          // 到达终点后，不再需要计算旋转，且防止后续误判
        } else {
          // 正常移动
          u.x += u.speedX * this.delta;
          u.y += u.speedY * this.delta;
          u.rotation = u.angle - 90;
        }
      }

      if (u.velocityDirty && Mathf.len(u.speedX, u.speedY) > 0.01f) {

        u.rotation = u.angle - 90;
      }

      // 网格更新
      if (u.x != oldX || u.y != oldY) {
        updateChunkPosition(u);
      }
    }
  }

  private void updateChunkPosition(Unit u) {
    if (WorldData.unitGrid == null) return;

    int newIndex = WorldData.getChunkIndex(u.x, u.y);

    if (newIndex < 0 || newIndex >= WorldData.unitGrid.length) return;

    if (newIndex != u.currentChunkIndex) {
      if (u.currentChunkIndex != -1 && u.currentChunkIndex < WorldData.unitGrid.length) {
        // 由于 UnitProces 是唯一修改位置和网格归属的系统，
        // 且 WorldData.unitGrid 通常只被用于读取(点击检测)，
        // 这里不加锁通常是可行的。但如果出现并发修改异常，请在这里加 synchronized
        WorldData.unitGrid[u.currentChunkIndex].remove(u);
        u.team.data().updateChunk(u, u.currentChunkIndex, newIndex);
      }

      WorldData.unitGrid[newIndex].add(u);
      u.currentChunkIndex = newIndex;
    }
  }
}
