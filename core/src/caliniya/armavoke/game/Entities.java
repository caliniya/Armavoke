package caliniya.armavoke.game;

import arc.func.Boolf;
import arc.func.Cons;
import arc.math.Mathf;
import arc.struct.IntQueue;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.data.*;

public class Entities {

  // --- ID 管理系统 ---
  private static int lastEntityID = 0;
  private static final IntQueue freeIDs = new IntQueue();

  /** 分配一个唯一的实体ID。 优先重用已回收的ID，否则生成新ID。 */
  public static int assignID() {
    if (freeIDs.size > 0) {
      return freeIDs.removeFirst();
    }
    return ++lastEntityID;
  }
  
  /** 回收一个实体ID，供后续实体重用。
   同时直接返回-1便于调用 */
  public static int freeID(int id) {
    if (id > 0 && id <= lastEntityID) {
      freeIDs.addLast(id);
    }
    return -1;
  }

  /** 重置ID系统。 */
  public static void clearIDs() {
    lastEntityID = 0;
    freeIDs.clear();
  }

  // --- 索敌逻辑（以实体为基础） ---

  /** 在指定范围内查找所有敌人实体 */
  public static void nearbyEnemies(
      TeamTypes sourceTeam, float x, float y, float radius, Cons<Entity> consumer) {
    for (TeamTypes otherTeam : TeamTypes.values()) {
      if (otherTeam == sourceTeam) continue;
      if (otherTeam == TeamTypes.Abort) continue;

      TeamData data = Teams.get(otherTeam);
      if (data == null) continue;

      data.find(x, y, radius, u -> consumer.get(u));
    }
  }

  /** 查找最近的敌人实体 */
  public static Entity closestEnemy(TeamTypes sourceTeam, float x, float y, float radius) {
    final Object[] result = {null};
    final float[] minDst2 = {radius * radius};

    nearbyEnemies(
        sourceTeam,
        x,
        y,
        radius,
        enemy -> {
          float dst2 = Mathf.dst2(x, y, enemy.x, enemy.y);
          if (dst2 < minDst2[0]) {
            minDst2[0] = dst2;
            result[0] = enemy;
          }
        });

    return (Entity) result[0];
  }
  
  /** 查找最近的敌人实体 */
  public static void closestEnemy(TeamTypes sourceTeam, float x, float y, float radius , Cons<Entity> con) {
    final Object[] result = {null};
    final float[] minDst2 = {radius * radius};

    nearbyEnemies(
        sourceTeam,
        x,
        y,
        radius,
        enemy -> {
          float dst2 = Mathf.dst2(x, y, enemy.x, enemy.y);
          if (dst2 < minDst2[0]) {
            minDst2[0] = dst2;
            result[0] = enemy;
          }
        });
        con.get((Entity)result[0]);
  }

  /**
   * 查找最近的敌人实体（带过滤 + 回调）
   * @param filter 过滤条件，返回 true 才纳入考虑
   * @param consumer 最近实体通过此回调传出，不直接返回值
   */
  public static void closestEnemy(
      TeamTypes sourceTeam, float x, float y, float radius,
      Boolf<Entity> filter, Cons<Entity> consumer) {
    final Object[] result = {null};
    final float[] minDst2 = {radius * radius};

    nearbyEnemies(
        sourceTeam,
        x,
        y,
        radius,
        enemy -> {
          if (!filter.get(enemy)) return;
          float dst2 = Mathf.dst2(x, y, enemy.x, enemy.y);
          if (dst2 < minDst2[0]) {
            minDst2[0] = dst2;
            result[0] = enemy;
          }
        });

    if (result[0] != null) {
      consumer.get((Entity) result[0]);
    }
  }
}
