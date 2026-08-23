package caliniya.armavoke.type.type;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.effect.Fx;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.ecs.runtime.EcsBulletRuntime;
import caliniya.armavoke.type.Bullet;

/** Immutable bullet content definition. Bullet instances are generated ECS entities. */
public class BulletType {
  public float speed = 6f;
  public float damage = 50f;
  public DamageType damageType = DamageType.Kinetic;
  public boolean breakArmor;
  public boolean bypassArmor;
  public boolean breakShield;
  public boolean bypassShield;
  public float knock;
  public float lifetime = 600f;
  public float size = 60f;
  public float drawSize = 1f;
  public Color frontColor = Color.white;
  public Color backColor = Color.gray;
  public TextureRegion region;

  public BulletType() {}

  public void load() { region = Core.atlas.find("bullet"); }

  public Bullet create(Entity owner, float x, float y, float rotation) {
    return EcsBulletRuntime.create(this, owner, x, y, rotation);
  }

  public void update(Bullet bullet) {}

  public void draw(Bullet bullet) {
    if (region == null || bullet == null) return;
    Draw.color(backColor);
    Draw.rect(region, bullet.x(), bullet.y(), size * 1.5f * drawSize, size * 1.5f * drawSize,
        bullet.rotation() - 90f);
    Draw.color(frontColor);
    Draw.rect(region, bullet.x(), bullet.y(), size * drawSize, size * drawSize,
        bullet.rotation() - 90f);
    Draw.color();
  }

  public void hit(Bullet bullet, Entity target) {
    if (bullet != null) Fx.hit.at(bullet.x(), bullet.y(), bullet.rotation(), frontColor, target);
  }

  public void despawn(Bullet bullet) { if (bullet != null) bullet.remove(); }
}
