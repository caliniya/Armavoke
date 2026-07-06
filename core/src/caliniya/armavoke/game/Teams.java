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

  public static TeamData get(TeamTypes team) {
    if (datas == null || team == null) return null;
    return datas[team.ordinal()];
  }

  /** 注册实体到团队 */
  public static void add(Entity e) {
    if (e.team != null) {
      TeamData data = get(e.team);
      if (data != null && !data.entities.contains(e)) {
        data.entities.add(e);
      }
    }
  }

  /** 从团队注销实体 */
  public static void remove(Entity e) {
    if (e.team != null) {
      TeamData data = get(e.team);
      if (data != null) {
        data.entities.remove(e);
      }
    }
  }
}
