package caliniya.armavoke.core;

import arc.util.Log;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.*;

public class Teams {
    private static TeamData[] datas;

    /** 
     * 初始化/重置团队数据
     * 必须在 WorldData.initWorld 或 rebuildall 时调用
     */
    public static void init() {
        TeamTypes[] allTeams = TeamTypes.values();
        datas = new TeamData[allTeams.length];
        
        Log.info(allTeams.length);
        for (int i = 0; i < allTeams.length; i++) {
            datas[i] = new TeamData(allTeams[i]);
        }
    }

    /** 获取指定团队的数据 */
    public static TeamData get(TeamTypes team) {
        // 安全检查
        if (datas == null || team == null) return null;
        return datas[team.ordinal()];
    }

    /** 注册单位到团队 (通常在 Unit.init 或 Unit.read 后调用) */
    public static void add(Unit u) {
        if (u.team != null) {
            TeamData data = get(u.team);
            if (data != null && !data.units.contains(u)) {
                data.units.add(u);
            }
        }
    }

    /** 从团队注销单位 (通常在 Unit.remove 时调用) */
    public static void remove(Unit u) {
        if (u.team != null) {
            TeamData data = get(u.team);
            if (data != null) {
                data.units.remove(u);
            }
        }
    }
}