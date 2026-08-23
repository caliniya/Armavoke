package caliniya.armavoke.ecs.definition;

import caliniya.armavoke.annotations.EntityDef;
import caliniya.armavoke.annotations.ThreadDef;

public final class GameEcsDefinitions {
  private GameEcsDefinitions() {}

  @ThreadDef(name = "main", workers = 1, priority = Thread.NORM_PRIORITY)
  public static final class MainThread {}

  @ThreadDef(name = "path", workers = 1, priority = Thread.NORM_PRIORITY - 1)
  public static final class PathThread {}

  @ThreadDef(name = "simulation", workers = 2, priority = Thread.NORM_PRIORITY)
  public static final class SimulationThread {}

  @ThreadDef(name = "ai", workers = 1, priority = Thread.NORM_PRIORITY - 1)
  public static final class AiThread {}

  @EntityDef(
      name = "unit",
      generatedClass = "UnitEcsEntity",
      components = {
        GameComponents.Position.class,
        GameComponents.Health.class,
        GameComponents.Armor.class,
        GameComponents.Movement.class,
        GameComponents.Energy.class,
        GameComponents.Weapon.class,
        GameComponents.Unit.class,
        GameComponents.Team.class,
        GameComponents.Targeting.class,
        GameComponents.AiControl.class,
        GameComponents.Pathfinding.class,
        GameComponents.Collision.class
      })
  public static final class UnitEntity {}

  @EntityDef(
      name = "building",
      generatedClass = "BuildingEcsEntity",
      components = {
        GameComponents.Position.class,
        GameComponents.Health.class,
        GameComponents.Armor.class,
        GameComponents.Energy.class,
        GameComponents.Weapon.class,
        GameComponents.Building.class,
        GameComponents.Team.class,
        GameComponents.Targeting.class,
        GameComponents.Production.class,
        GameComponents.Consumption.class,
        GameComponents.Spawner.class,
        GameComponents.Collision.class
      })
  public static final class BuildingEntity {}

  @EntityDef(
      name = "bullet",
      generatedClass = "BulletEcsEntity",
      components = {
        GameComponents.Position.class,
        GameComponents.Bullet.class,
        GameComponents.Team.class,
        GameComponents.Collision.class
      })
  public static final class BulletEntity {}

  @EntityDef(
      name = "effect",
      generatedClass = "EffectEcsEntity",
      serializable = false,
      components = {GameComponents.Position.class, GameComponents.Effect.class})
  public static final class EffectEntity {}
}
