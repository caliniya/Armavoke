package caliniya.armavoke.ecs.definition;

import caliniya.armavoke.annotations.AccessMode;
import caliniya.armavoke.annotations.Component;
import caliniya.armavoke.annotations.FieldOpt;
import caliniya.armavoke.annotations.Import;
import caliniya.armavoke.annotations.Storage;
import caliniya.armavoke.annotations.UpdateMethod;

public final class GameComponents {
  private GameComponents() {}

  @Component(name = "Position")
  public static final class Position {
    @FieldOpt(volatileField = true)
    public float x;

    @FieldOpt(volatileField = true)
    public float y;

    @FieldOpt(volatileField = true)
    public float rotation;
  }

  @Component(name = "Health")
  public static final class Health {
    @FieldOpt(defaultValue = "100f")
    public float health;

    @FieldOpt(defaultValue = "100f")
    public float maxHealth;
  }

  @Component(name = "Armor")
  public static final class Armor {
    public float armor;
    public float maxArmor;
    public float armorValue;
  }

  @Component(name = "Movement", updateBy = "movement", requires = Position.class)
  @Import(component = Position.class, fields = {"x", "y"}, mode = AccessMode.ReadWrite)
  public static final class Movement {
    @FieldOpt(volatileField = true)
    public float velocityX;

    @FieldOpt(volatileField = true)
    public float velocityY;

    @FieldOpt(volatileField = true)
    public float targetX;

    @FieldOpt(volatileField = true)
    public float targetY;

    @FieldOpt(defaultValue = "1f")
    public float speed;

    public boolean moving;
  }

  @Component(
      name = "Energy",
      updateBy = "general",
      pure = false,
      pooled = true,
      storage = Storage.Reference)
  public static final class Energy {
    public float current;
    public float max;
    public float regen;

    @UpdateMethod
    public void update(float delta) {
      if (regen != 0f && current < max) current = Math.min(max, current + regen / 60f * delta);
    }
  }

  @Component(name = "Weapon", requires = {Position.class, Team.class})
  public static final class Weapon {
    @FieldOpt(defaultValue = "-1")
    public int weaponTypeId;

    public float reload;

    @FieldOpt(defaultValue = "60f")
    public float reloadTime;

    @FieldOpt(defaultValue = "-1")
    public int targetId;

    @FieldOpt(defaultValue = "150f")
    public float range;
  }

  @Component(name = "Bullet", updateBy = "bullet", requires = {Position.class, Team.class})
  public static final class Bullet {
    public float damage;

    @FieldOpt(defaultValue = "1f")
    public float speed;

    public float directionX;
    public float directionY;

    @FieldOpt(defaultValue = "60f")
    public float lifetime;

    public float time;
  }

  @Component(name = "Building")
  public static final class Building {
    @FieldOpt(defaultValue = "-1")
    public int blockId;

    @FieldOpt(defaultValue = "32f")
    public float size;
  }

  @Component(name = "Unit")
  public static final class Unit {
    @FieldOpt(defaultValue = "-1")
    public int unitTypeId;

    @FieldOpt(defaultValue = "12f")
    public float size;
  }

  @Component(
      name = "Effect",
      updateBy = "general",
      requires = Position.class,
      pure = false,
      pooled = true,
      storage = Storage.Reference)
  public static final class Effect {
    @FieldOpt(defaultValue = "-1")
    public int effectId;

    public float time;

    @FieldOpt(defaultValue = "30f")
    public float lifetime;

    @FieldOpt(defaultValue = "64f")
    public float clip;

    @UpdateMethod
    public void update(float delta) {
      time += delta;
    }
  }

  @Component(
      name = "Pathfinding",
      updateBy = "pathfinding",
      requires = {Position.class, Movement.class})
  @Import(component = Position.class, fields = {"x", "y"})
  public static final class Pathfinding {
    public float targetX;
    public float targetY;
    public int routeVersion;
    public boolean repath;
  }

  @Component(name = "Team")
  public static final class Team {
    @FieldOpt(defaultValue = "-1")
    public int teamId;
  }

  @Component(
      name = "Targeting",
      updateBy = "targeting",
      requires = {Position.class, Team.class})
  public static final class Targeting {
    @FieldOpt(defaultValue = "-1")
    public int targetId;

    @FieldOpt(defaultValue = "150f")
    public float range;

    public float scanTimer;

    @FieldOpt(defaultValue = "15f")
    public float scanInterval;
  }

  @Component(
      name = "AiControl",
      updateBy = "ai",
      requires = {Targeting.class, Movement.class})
  @Import(component = Targeting.class, fields = "targetId", mode = AccessMode.ReadOnly)
  public static final class AiControl {
    public int state;
    public float thinkTimer;
  }

  @Component(
      name = "Production",
      updateBy = "general",
      requires = Consumption.class,
      pure = false,
      storage = Storage.Reference)
  public static final class Production {
    public float progress;

    @FieldOpt(defaultValue = "300f")
    public float craftTime;

    @FieldOpt(defaultValue = "-1")
    public int recipeId;

    public boolean crafting;

    @UpdateMethod(order = 10)
    public void update(float delta) {
      if (crafting) progress += delta;
    }
  }

  @Component(name = "Consumption")
  public static final class Consumption {
    public int itemCost;
    public float powerCost;
    public float liquidCost;
  }

  @Component(name = "Spawner", requires = Production.class)
  public static final class Spawner {
    @FieldOpt(defaultValue = "-1")
    public int outputTypeId;

    public int outputCount;
  }

  @Component(name = "Collision", requires = Position.class)
  public static final class Collision {
    @FieldOpt(defaultValue = "8f")
    public float width;

    @FieldOpt(defaultValue = "8f")
    public float height;

    @FieldOpt(defaultValue = "true")
    public boolean solid;
  }
}
