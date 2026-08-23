package caliniya.armavoke.game.data;

import arc.func.Cons;
import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.ecs.runtime.EcsEntity;

public class TeamData {
  public final TeamTypes team;

  public TeamData(TeamTypes team) { this.team = team; }

  public void find(float x, float y, float radius, Cons<Entity> consumer) {
    float range2 = radius * radius;
    for (EcsEntity value : EcsQueries.snapshot()) {
      if (value instanceof Entity entity && entity.active() && entity.team() == team
          && Mathf.dst2(x, y, entity.x(), entity.y()) <= range2) consumer.get(entity);
    }
  }
}
