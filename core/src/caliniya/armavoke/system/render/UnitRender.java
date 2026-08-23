package caliniya.armavoke.system.render;

import arc.Core;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.system.System;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.Unit;

/** Renders authoritative ECS units and bullets. */
public class UnitRender extends System<UnitRender> {
  public static boolean debug = true;
  public static Ar<Bullet> temp = new Ar<>(false, 1000);

  @Override public UnitRender init() { return this; }

  @Override
  public void update() {
    for (Unit unit : EcsQueries.units()) {
      if (unit.health() <= 0f || !shouldDraw(unit.x(), unit.y(), unit.size() * 2f)) continue;
      unit.draw();
      if (unit.type() != null) {
        unit.type().drawHealthBar(unit);
        if (debug) unit.type().drawDebug(unit);
      }
    }
    for (Bullet bullet : EcsQueries.bullets()) if (shouldDraw(bullet.x(), bullet.y(), bullet.collisionWidth() * 2f)) bullet.draw();
  }

  private boolean shouldDraw(float x, float y, float padding) {
    float width = Core.camera.width * 0.5f + padding;
    float height = Core.camera.height * 0.5f + padding;
    return Math.abs(x - Core.camera.position.x) <= width && Math.abs(y - Core.camera.position.y) <= height;
  }
}
