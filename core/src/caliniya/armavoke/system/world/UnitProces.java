package caliniya.armavoke.system.world;

import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;

// 单位处理(意图处理) TODO
// 在后台运行
public class UnitProces extends BasicSystem<UnitProces> {
  
  public static UnitProces i;
  
  @Override
  public UnitProces init() {
    i = super.init(true);
    return i;
  }

  @Override
  public void update() {
    Ar<Unit> list = WorldData.units;

    for (int i = 0; i < list.size; i++) {
      Unit u = list.get(i);
      if (u == null) continue;
      if (u.health <= 0) {
          u.remove(); 
          continue;
      }
      
      // 暂时强制所有单位开火 (测试用)
      u.shooting = true;
      
      // TODO: 未来在这里实现:
      // 1. 索敌 (Find Target)
      // 2. 避障力计算 (Separation)
      // 3. 状态机切换 (Idle -> Moving -> Attacking)
    }
  }
  
  public static UnitProces getThis() {
  	return i;
  }
}