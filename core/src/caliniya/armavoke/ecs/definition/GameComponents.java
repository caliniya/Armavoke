package caliniya.armavoke.ecs.definition;

import arc.math.geom.Point2;
import caliniya.armavoke.annotations.Component;
import caliniya.armavoke.annotations.FieldOpt;
import caliniya.armavoke.annotations.Storage;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.type.Enhancement;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.type.enhance.api.Updatable;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.type.module.LiquidModule;
import caliniya.armavoke.type.module.PowerModule;

/** Component declarations for every piece of mutable gameplay state. */
public final class GameComponents {
  private GameComponents() {}

  @Component(name = "Position")
  public static class Position {
    @FieldOpt(volatileField = true) public float x;
    @FieldOpt(volatileField = true) public float y;
    @FieldOpt(volatileField = true) public float rotation;
  }

  @Component(name = "Health")
  public static class Health {
    @FieldOpt(defaultValue = "100f") public float health;
    @FieldOpt(defaultValue = "100f") public float maxHealth;
  }

  @Component(name = "Armor")
  public static class Armor {
    public float armor;
    public float maxArmor;
    public float armorValue;
  }

  @Component(name = "Movement", updateBy = "movement", requires = Position.class)
  public static class Movement {
    @FieldOpt(volatileField = true) public float velocityX;
    @FieldOpt(volatileField = true) public float velocityY;
    @FieldOpt(volatileField = true) public float targetX;
    @FieldOpt(volatileField = true) public float targetY;
    @FieldOpt(defaultValue = "1f") public float speed;
    public boolean moving;
  }

  @Component(name = "Energy", storage = Storage.Reference)
  public static class Energy {
    public float current;
    public float max;
    public float regen;
  }

  @Component(name = "Inventory", storage = Storage.Reference)
  public static class Inventory {
    @FieldOpt(defaultValue = "new caliniya.armavoke.type.module.ItemModule(100)", persist = false)
    public ItemModule items;
    @FieldOpt(defaultValue = "new caliniya.armavoke.type.module.LiquidModule(100f)", persist = false)
    public LiquidModule liquids;
    @FieldOpt(defaultValue = "new caliniya.armavoke.type.module.PowerModule(100f)", persist = false)
    public PowerModule power;
  }

  @Component(name = "RuntimeData", storage = Storage.Reference)
  public static class RuntimeData {
    @FieldOpt(defaultValue = "new caliniya.armavoke.base.tool.Ar<>()", persist = false)
    public Ar<Ability> abilities;
    @FieldOpt(defaultValue = "new caliniya.armavoke.base.tool.Ar<>()", persist = false)
    public Ar<Enhancement> enhancements;
    @FieldOpt(defaultValue = "new caliniya.armavoke.base.tool.Ar<>()", persist = false)
    public Ar<Updatable> updatables;
    @FieldOpt(defaultValue = "new caliniya.armavoke.base.tool.Ar<>()", persist = false)
    public Ar<caliniya.armavoke.type.Weapon> weapons;
    @FieldOpt(defaultValue = "new caliniya.armavoke.base.tool.Ar<>()", persist = false)
    public Ar<Point2> path;
    public int pathIndex;
  }

  @Component(name = "Combat", storage = Storage.Reference)
  public static class Combat {
    public float heat;
    @FieldOpt(defaultValue = "100f") public float heatMax;
    @FieldOpt(defaultValue = "1f") public float heatSpeed;
    @FieldOpt(defaultValue = "true") public boolean heatable;
    public boolean locked;
    public float knockX;
    public float knockY;
    @FieldOpt(defaultValue = "new float[caliniya.armavoke.base.type.DamageType.values().length]", persist = false)
    public float[] armorResist;
  }

  @Component(name = "Weapon", requires = {Position.class, Team.class})
  public static class Weapon {
    @FieldOpt(defaultValue = "-1") public int weaponTypeId;
    public float reload;
    @FieldOpt(defaultValue = "60f") public float reloadTime;
    @FieldOpt(defaultValue = "-1") public int targetId;
    @FieldOpt(defaultValue = "150f") public float range;
  }

  @Component(name = "Bullet", updateBy = "bullet", requires = {Position.class, Team.class})
  public static class Bullet {
    @FieldOpt(defaultValue = "-1") public int bulletTypeId;
    @FieldOpt(defaultValue = "-1") public int ownerId;
    public float damage;
    @FieldOpt(defaultValue = "1f") public float speed;
    public float directionX;
    public float directionY;
    @FieldOpt(defaultValue = "60f") public float lifetime;
    public float time;
  }

  @Component(name = "Building")
  public static class Building {
    @FieldOpt(defaultValue = "-1") public int blockId;
    @FieldOpt(defaultValue = "32f") public float size;
    public int tileX;
    public int tileY;
    public int angle;
  }

  @Component(name = "Unit")
  public static class Unit {
    @FieldOpt(defaultValue = "-1") public int unitTypeId;
    @FieldOpt(defaultValue = "12f") public float size;
    public boolean selected;
  }

  @Component(name = "Effect", updateBy = "general", requires = Position.class)
  public static class Effect {
    @FieldOpt(defaultValue = "-1") public int effectId;
    public float time;
    @FieldOpt(defaultValue = "60f") public float lifetime;
    public float data;
    public int colorRgba;
  }

  @Component(name = "Pathfinding", updateBy = "pathfinding", requires = {Position.class, Movement.class})
  public static class Pathfinding {
    public float targetX;
    public float targetY;
    public int routeVersion;
    public boolean repath;
  }

  @Component(name = "Team")
  public static class Team {
    @FieldOpt(defaultValue = "-1") public int teamId;
  }

  @Component(name = "Targeting", updateBy = "targeting", requires = {Position.class, Team.class})
  public static class Targeting {
    @FieldOpt(defaultValue = "-1") public int targetId;
    @FieldOpt(defaultValue = "150f") public float range;
    public float scanTimer;
    @FieldOpt(defaultValue = "15f") public float scanInterval;
  }

  @Component(name = "AiControl", updateBy = "ai", requires = {Unit.class, Movement.class})
  public static class AiControl {
    public int state;
    public float thinkTimer;
  }

  @Component(name = "Production", updateBy = "general", storage = Storage.Reference)
  public static class Production {
    public float progress;
    @FieldOpt(defaultValue = "300f") public float craftTime;
    @FieldOpt(defaultValue = "-1") public int recipeId;
    public boolean crafting;
  }

  @Component(name = "Consumption")
  public static class Consumption {
    public int itemCost;
    public float powerCost;
    public float liquidCost;
  }

  @Component(name = "Spawner", requires = Production.class)
  public static class Spawner {
    @FieldOpt(defaultValue = "-1") public int outputTypeId;
    public int outputCount;
  }

  @Component(name = "Collision", updateBy = "collision", requires = Position.class)
  public static class Collision {
    @FieldOpt(defaultValue = "8f") public float width;
    @FieldOpt(defaultValue = "8f") public float height;
    @FieldOpt(defaultValue = "true") public boolean solid;
  }
}
