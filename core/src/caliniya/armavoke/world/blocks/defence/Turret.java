package caliniya.armavoke.world.defence.turret;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.ecs.runtime.EcsBulletRuntime;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.type.BulletType;
import caliniya.armavoke.world.Block;

/** Turret behavior whose mutable state lives in building ECS components. */
public class Turret extends Block {
  public float range = 400f;
  public float rotateSpeed = 500f;
  public float reloadTime = 10f;
  public BulletType bulletType;
  public TextureRegion baseRegion;

  public Turret(String name) { super(name); }

  @Override
  public void load() {
    super.load();
    baseRegion = Core.atlas.find(name + "-base");
  }

  @Override
  public void update(Building building, float delta) {
    Entity target = building.target();
    if (target == null || !target.active() || target.health() <= 0f
        || Mathf.dst2(building.x(), building.y(), target.x(), target.y()) > range * range) {
      target = findTarget(building);
      building.target(target);
    }
    building.weaponReload(building.weaponReload() + delta);
    if (target == null) return;
    float angle = Angles.angle(building.x(), building.y(), target.x(), target.y());
    building.rotation(Angles.moveToward(building.rotation(), angle, rotateSpeed * delta));
    if (bulletType != null && building.weaponReload() >= reloadTime) {
      EcsBulletRuntime.create(bulletType, building, building.x(), building.y(), building.rotation());
      building.weaponReload(0f);
    }
  }

  @Override
  public void draw(Building building) {
    if (baseRegion != null) Draw.rect(baseRegion, building.x(), building.y(), building.angle() * 90f);
    Draw.rect(region, building.x(), building.y(), building.rotation() - 90f);
  }

  @Override
  public Entity findTarget(Building building) {
    return Entities.closestEnemy(building.team(), building.x(), building.y(), range);
  }

  @Override public void write(Building building, Writes writes) {}
  @Override public void read(Building building, Reads reads) {}

  @Override
  public void drawDebug(Building building) {
    super.drawDebug(building);
    Draw.color(Color.scarlet);
    Lines.circle(building.x(), building.y(), range);
    Draw.color();
  }
}
