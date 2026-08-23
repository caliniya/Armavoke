package caliniya.armavoke.ecs.definition;

import caliniya.armavoke.annotations.EntityDef;
import caliniya.armavoke.annotations.ThreadDef;

public final class GameEcsDefinitions {
  private GameEcsDefinitions() {}

  @ThreadDef(name = "main", workers = 1, interruptible = false)
  public static final class MainThread {}

  @ThreadDef(name = "path", workers = 2)
  public static final class PathThread {}

  @ThreadDef(name = "simulation", workers = 2)
  public static final class SimulationThread {}

  @ThreadDef(name = "ai", workers = 1)
  public static final class AiThread {}

  @EntityDef(
      name = "unit",
      interfaces = "caliniya.armavoke.type.Unit",
      components = {
        GameComponents.Position.class, GameComponents.Health.class,
        GameComponents.Armor.class, GameComponents.Movement.class,
        GameComponents.Energy.class, GameComponents.Inventory.class,
        GameComponents.RuntimeData.class, GameComponents.Combat.class,
        GameComponents.Weapon.class, GameComponents.Unit.class,
        GameComponents.Team.class, GameComponents.Targeting.class,
        GameComponents.AiControl.class, GameComponents.Pathfinding.class,
        GameComponents.Collision.class
      },
      generatedClass = "UnitEcsEntity")
  public static final class UnitEntity {}

  @EntityDef(
      name = "building",
      interfaces = "caliniya.armavoke.type.Building",
      components = {
        GameComponents.Position.class, GameComponents.Health.class,
        GameComponents.Armor.class, GameComponents.Energy.class,
        GameComponents.Inventory.class, GameComponents.RuntimeData.class,
        GameComponents.Combat.class, GameComponents.Weapon.class,
        GameComponents.Building.class, GameComponents.Team.class,
        GameComponents.Targeting.class, GameComponents.Production.class,
        GameComponents.Consumption.class, GameComponents.Spawner.class,
        GameComponents.Collision.class
      },
      generatedClass = "BuildingEcsEntity")
  public static final class BuildingEntity {}

  @EntityDef(
      name = "bullet",
      interfaces = "caliniya.armavoke.type.Bullet",
      components = {
        GameComponents.Position.class, GameComponents.Bullet.class,
        GameComponents.Team.class, GameComponents.Collision.class
      },
      serializable = false,
      generatedClass = "BulletEcsEntity")
  public static final class BulletEntity {}

  @EntityDef(
      name = "effect",
      components = {GameComponents.Position.class, GameComponents.Effect.class},
      serializable = false,
      generatedClass = "EffectEcsEntity")
  public static final class EffectEntity {}
}
