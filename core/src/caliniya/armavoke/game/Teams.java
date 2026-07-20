package caliniya.armavoke.game;

import arc.util.Log;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.game.data.*;

public class Teams {
  private static TeamData[] datas;

  public static void init() {
    TeamTypes[] allTeams = TeamTypes.values();
    datas = new TeamData[allTeams.length];
    
    for (int i = 0; i < allTeams.length; i++) {
      datas[i] = new TeamData(allTeams[i]);
    }
  }

  /** 初始化所有团队的四叉树范围 */
  public static void initAllTrees(float worldPixelW, float worldPixelH) {
    for (TeamData data : datas) {
      data.initTree(worldPixelW, worldPixelH);
    }
  }

  public static TeamData get(TeamTypes team) {
    if (datas == null || team == null) return null;
    return datas[team.ordinal()];
  }

  /** 注册实体到团队（同时加入 EntityGroup 和全局列表） */
  public static void add(Entity e) {
    if (e.team != null) {
      TeamData data = get(e.team);
      if (data != null && !data.entities.contains(e)) {
        data.entities.add(e);
        data.entityGroup.add(e);
      }
    }
  }

  /** 从团队注销实体（同时从 EntityGroup 和全局列表移除） */
  public static void remove(Entity e) {
    if (e.team != null) {
      TeamData data = get(e.team);
      if (data != null) {
        data.entities.remove(e);
        data.entityGroup.remove(e);
      }
    }
  }
}
