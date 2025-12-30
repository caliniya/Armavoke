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

      // -----------------------------------------------------------
      // 【测试代码】 强制开火逻辑
      // -----------------------------------------------------------
      
      // 设置强制射击状态
      u.shooting = true; 
      
      float aimX = u.targetX;
      float aimY = u.targetY;
      
      if (u.targetX == 0 && u.targetY == 0) {
          aimX = u.x + 100;
          aimY = u.y;
      }
      
      u.updateWeapons();

      float oldX = u.x;
      float oldY = u.y;

      if (!(u.path == null) && !u.path.isEmpty()) {
        float distToTarget = Mathf.dst(u.x, u.y, u.targetX, u.targetY);
        if (distToTarget <= u.speed * Time.delta) {
          u.x = u.targetX;
          u.y = u.targetY;
          u.speedX = 0;
          u.speedY = 0;
        } else {
          u.x += u.speedX * Time.delta;
          u.y += u.speedY * Time.delta;
        }

        if (!u.shooting) {
          u.rotation = Angles.moveToward(u.rotation, u.angle - 90, u.rotationSpeed * Time.delta);
        }
        
        if (u.shooting) {
             float angleToTarget = Angles.angle(u.x, u.y, u.targetX, u.targetY);
             u.rotation = Angles.moveToward(u.rotation, angleToTarget - 90, u.rotationSpeed * Time.delta);
        }

        if (u.x != oldX || u.y != oldY) {
          updateChunkPosition(u);
        }
      }
    }
  }


  private void updateChunkPosition(Unit u) {
    if (WorldData.unitGrid == null) return;
    int newIndex = WorldData.getChunkIndex(u.x, u.y);
    if (newIndex < 0 || newIndex >= WorldData.unitGrid.length) return;
    if (newIndex != u.currentChunkIndex) {
      if (u.currentChunkIndex != -1 && u.currentChunkIndex < WorldData.unitGrid.length) {
        WorldData.unitGrid[u.currentChunkIndex].remove(u);
        u.team.data().updateChunk(u, u.currentChunkIndex, newIndex);
      }
      WorldData.unitGrid[newIndex].add(u);
      u.currentChunkIndex = newIndex;
    }
  }
}