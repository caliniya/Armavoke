package caliniya.armavoke.game.data;

import arc.func.Cons;
import arc.math.Mathf;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.base.game.Entity;

/**
 * 团队数据 —— 管理该团队下所有实体的集合。
 */
public class TeamData {
  public final TeamTypes team;

  /** 该团队下的所有实体列表（全局遍历用） */
  public final Ar<Entity> entities = new Ar<>();

  /** 带四叉树空间索引的实体组 —— 用于空间查询和遍历 */
  public final EntityAr<Entity> entityGroup;

  public TeamData(TeamTypes team) {
    this.team = team;
    this.entityGroup = new EntityAr<>(true, false, e -> e.id);
  }

  /** 在世界初始化后调用，设置四叉树的覆盖范围 */
  public void initTree(float worldPixelW, float worldPixelH) {
    entityGroup.resize(0, 0, worldPixelW, worldPixelH);
  }

  /**
   * 在矩形范围内查找本团队的实体。
   * 使用四叉树，效率远高于遍历全局列表。
   *
   * @param output 结果集，调用者负责清空
   */
  public void get(float minX, float minY, float maxX, float maxY, Ar<Entity> output) {
    if (entityGroup.isEmpty()) return;

    float w = maxX - minX;
    float h = maxY - minY;
    float cx = minX + w / 2f;
    float cy = minY + h / 2f;

    if (entityGroup.useTree()) {
      // 四叉树查询：直接用中心点 + 半宽半高的形式
      entityGroup.intersect(minX, minY, w, h, entity -> {
        // QuadTree 的 intersect 不会产生 false negatives，但可能有少量 false
        // positives（因为用的是粗略包围盒）。这里再做精确过滤。
        if (entity != null && entity.x >= minX && entity.x <= maxX
            && entity.y >= minY && entity.y <= maxY) {
          output.add(entity);
        }
      });
    } else {
      for (int i = 0; i < entities.size; i++) {
        Entity e = entities.get(i);
        if (e != null && e.x >= minX && e.x <= maxX && e.y >= minY && e.y <= maxY) {
          output.add(e);
        }
      }
    }
  }

  /** 在指定圆形范围内查找本团队的实体 */
  public void find(float x, float y, float radius, Cons<Entity> consumer) {
    if (entityGroup.isEmpty()) return;

    float minX = x - radius;
    float minY = y - radius;
    float maxX = x + radius;
    float maxY = y + radius;
    float r2 = radius * radius;

    if (entityGroup.useTree()) {
      entityGroup.intersect(minX, minY, radius * 2, radius * 2, entity -> {
        if (entity != null && entity.health > 0
            && Mathf.dst2(x, y, entity.x, entity.y) <= r2) {
          consumer.get(entity);
        }
      });
    } else {
      for (int i = 0; i < entities.size; i++) {
        Entity e = entities.get(i);
        if (e == null || e.health <= 0) continue;
        if (e.x < minX || e.x > maxX || e.y < minY || e.y > maxY) continue;

        if (Mathf.dst2(x, y, e.x, e.y) <= r2) {
          consumer.get(e);
        }
      }
    }
  }
}
