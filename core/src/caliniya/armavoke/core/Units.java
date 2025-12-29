package caliniya.armavoke.core;

import arc.func.Cons;
import arc.math.Mathf;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.*;

public class Units {

    /**
     * 在指定范围内查找所有敌人
     * @param sourceTeam 搜索者的团队
     * @param x 搜索中心 X
     * @param y 搜索中心 Y
     * @param radius 搜索半径
     * @param consumer 对找到的每个敌人执行的操作
     */
    public static void nearbyEnemies(TeamTypes sourceTeam, float x, float y, float radius, Cons<Unit> consumer) {
        // 遍历所有可能的团队
        for (TeamTypes otherTeam : TeamTypes.values()) {
            // 排除自己
            if (otherTeam == sourceTeam) continue;
            
            //排除中立
            if (otherTeam == TeamTypes.Abort) continue;

            // 获取该团队的数据
            TeamData data = Teams.get(otherTeam);
            if (data == null) continue;

            // 在该敌对团队中进行空间搜索
            data.find(x, y, radius, consumer);
        }
    }

    /**
     * 查找最近的敌人
     * @return 最近的敌人，没找到则返回 null
     */
    public static Unit closestEnemy(TeamTypes sourceTeam, float x, float y, float radius) {
        // 使用单元素数组来存储结果 (为了在 Lambda 中修改)
        // 数组内容: [0]=最近的单位, [1]=最近距离的平方
        final Object[] result = {null};
        final float[] minDst2 = {radius * radius}; // 初始设为半径平方，超过半径的不算

        nearbyEnemies(sourceTeam, x, y, radius, enemy -> {
            float dst2 = Mathf.dst2(x, y, enemy.x, enemy.y);
            
            // 只有当更近时才更新
            if (dst2 < minDst2[0]) {
                minDst2[0] = dst2;
                result[0] = enemy;
            }
        });

        return (Unit) result[0];
    }
  
  public static void method() {
  	
  }
  
}