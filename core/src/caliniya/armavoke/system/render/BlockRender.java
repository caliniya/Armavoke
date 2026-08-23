package caliniya.armavoke.system.render;

import arc.Core;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.type.Building;

/** Renders authoritative ECS buildings. */
public class BlockRender extends caliniya.armavoke.system.System<BlockRender> {
  @Override public BlockRender init() { return this; }

  @Override
  public void update() {
    for (Building building : EcsQueries.buildings()) {
      if (building.health() <= 0f || building.block() == null || !shouldDraw(building.x(), building.y(), building.size())) continue;
      building.draw();
    }
  }

  private boolean shouldDraw(float x, float y, float padding) {
    float width = Core.camera.width * 0.5f + padding;
    float height = Core.camera.height * 0.5f + padding;
    return Math.abs(x - Core.camera.position.x) <= width && Math.abs(y - Core.camera.position.y) <= height;
  }
}
