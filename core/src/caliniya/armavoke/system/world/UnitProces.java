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
      
      // 1. 设置强制射击状态
      u.shooting = true; 
      
      // 2. 瞄准目标：如果有移动目标就打移动目标，否则打自己脚下(或者某个固定点)
      // 注意：updateWeapons 需要在主线程/逻辑线程调用，因为它可能产生子弹(涉及对象池)
      // 我们的 UnitProces 是在后台线程吗？super.init(true) 说是后台。
      // Bullet.create 会访问 WorldData.bullets，这是线程不安全的吗？
      // 如果 BulletSystem 也在后台且单线程处理，或者 WorldData.bullets 是线程安全的，那就没问题。
      // 建议：BulletSystem 和 UnitProces 最好在同一个线程，或者加锁。
      
      // 这里假设 UnitProces 是主要的逻辑驱动者
      
      // 让武器瞄准移动目标点
      float aimX = u.targetX;
      float aimY = u.targetY;
      
      // 如果没有移动目标(原地不动)，为了测试效果，可以让它转圈打
      if (u.targetX == 0 && u.targetY == 0) {
          aimX = u.x + 100;
          aimY = u.y;
      }

      // 更新所有武器状态 (瞄准 + 射击)
      // 你需要在 Unit 类里确保 updateWeapons 方法接受 (aimX, aimY, shootCommand)
      // 并且 Weapon.update 里会处理 reload 和 Bullet.create
      
      // 假设 Unit.updateWeapons 内部遍历调用 weapon.update(aimX, aimY, true)
      // 这里我们需要手动传参，或者修改 Unit.updateWeapons
      // 鉴于之前的 Unit 代码：
      // public void updateWeapons() { for (Weapon weapon : weapons) weapon.update(targetX, targetY, shooting); }
      // 所以我们只需要设置 u.shooting = true 即可。
      
      u.updateWeapons();

      // -----------------------------------------------------------
      // 物理移动逻辑 (保持不变)
      // -----------------------------------------------------------

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
        
        // 如果正在射击，rotation 由 Weapon 控制？或者单位本身需要朝向目标？
        // 如果是坦克，底盘(rotation)和炮塔分离，这里不用管。
        // 如果是步兵，射击时身体要朝向目标。
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

  // updateChunkPosition 保持不变...
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