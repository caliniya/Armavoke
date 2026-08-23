package caliniya.armavoke.type.type;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.api.DrawType;
import caliniya.armavoke.base.api.TechNodeContent;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.runtime.EcsEntityFactory;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.Weapon;
import caliniya.armavoke.type.ability.Ability;

/** Immutable unit content definition. Unit instances are generated ECS entities. */
public class UnitType extends ContentType implements DrawType<Unit>, TechNodeContent {
  public float speed = 60f, health = 100f, speedt, rotationSpeend = 1f;
  public float[] hitbox;
  public float size = 100f;
  public float scanDistance = 200f;
  public float engageRange = 150f;
  public int itemCap = 50;
  public float liquidCap;
  public float powerCap;
  public float armorMax;
  public float armorValue;
  public float[] armorResist = new float[DamageType.values().length];
  public float energyRegen;
  public float energyMax;
  public Ar<Ability> abilities = new Ar<>();
  public Ar<WeaponType> weapons = new Ar<>();
  public TextureRegion region, cell;

  public UnitType(String name) { super(name, CType.Unit); }

  @Override
  public TechNodeContent[] requirements() { return requirements; }

  public void load() {
    region = Core.atlas.find(name);
    cell = Core.atlas.find(name + "-cell");
    speedt = speed / 60f;
  }

  public Unit create(TeamTypes team, float x, float y) {
    return EcsEntityFactory.createUnit(this, team, x, y);
  }

  public Unit create() { return create(TeamTypes.Mutex, 0f, 0f); }

  @Override
  public void draw(Unit unit) {
    if (unit == null || region == null) return;
    for (Weapon weapon : unit.weapons()) weapon.draw();
    Draw.color();
    Draw.rect(region, unit.x(), unit.y(), size, size, unit.rotation() - 90f);
    if (cell != null && unit.team() != null) {
      Draw.color(Color.white);
      Draw.rect(cell, unit.x(), unit.y(), size, size, unit.rotation() - 90f);
      Draw.color();
    }
  }

  public void drawHealthBar(Unit unit) {
    if (unit == null || unit.maxHealth() <= 0f) return;
    float width = Math.max(28f, size * 0.8f);
    float y = unit.y() + size * 0.58f;
    Draw.color(Color.darkGray);
    Fill.rect(unit.x(), y, width, 5f);
    Draw.color(Color.valueOf("84f491"));
    Fill.rect(unit.x() - width * 0.5f + width * Mathf.clamp(unit.health() / unit.maxHealth()) * 0.5f,
        y, width * Mathf.clamp(unit.health() / unit.maxHealth()), 3f);
    Draw.color();
  }

  public void drawDebug(Unit unit) {
    Draw.color(Color.scarlet);
    Lines.rect(unit.x() - unit.width() * 0.5f, unit.y() - unit.height() * 0.5f,
        unit.width(), unit.height());
    if (unit.movementMoving()) Lines.line(unit.x(), unit.y(), unit.movementTargetX(), unit.movementTargetY());
    Draw.color();
  }

  public void update(Unit unit, float delta) {}

  public void addWeapons(WeaponType... newWeapons) {
    if (newWeapons == null) return;
    for (WeaponType weapon : newWeapons) {
      if (weapon == null) continue;
      weapons.add(weapon);
      if (weapon.mirror) {
        WeaponType mirrored = weapon.copy();
        mirrored.x = -weapon.x;
        mirrored.shootX = -weapon.shootX;
        mirrored.isMirror = true;
        weapon.otherSide = weapons.size;
        mirrored.otherSide = weapons.size - 1;
        weapons.add(mirrored);
      }
    }
  }
}
