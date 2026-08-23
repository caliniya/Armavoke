package caliniya.armavoke.ecs.runtime;

import arc.math.Angles;
import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.generated.access.AiControlAccess;
import caliniya.armavoke.ecs.generated.access.MovementAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.type.UnitType;

/** ECS-native unit creation and update phases. Unit remains a render/input view. */
public final class EcsUnitRuntime {
  private EcsUnitRuntime() {}

  public static Unit create(TeamTypes team, UnitType type, float x, float y) {
    Unit unit = Unit.createEcsView(team, type, x, y);
    Entities.add(unit);
    GameEcsBridge.register(unit);
    return unit;
  }

  public static Unit createUnbound(UnitType type) {
    return Unit.createEcsView(type);
  }

  public static void updateGeneral(EcsEntity entity, Unit unit, float delta) {
    float knockX = unit.knockX;
    float knockY = unit.knockY;
    if (knockX != 0f || knockY != 0f) {
      float maxX = WorldData.world.W * WorldData.TILE_SIZE;
      float maxY = WorldData.world.H * WorldData.TILE_SIZE;
      unit.x = Mathf.clamp(unit.x + knockX * delta, 0f, maxX);
      unit.y = Mathf.clamp(unit.y + knockY * delta, 0f, maxY);
      unit.knockX = Mathf.lerpDelta(unit.knockX, 0f, unit.knockDamp);
      unit.knockY = Mathf.lerpDelta(unit.knockY, 0f, unit.knockDamp);
      unit.velocityDirty = true;
      unit.refreshEcsHitbox();
    }
    unit.updateBase(delta);
    GameEcsBridge.syncFromLegacy(unit);
  }

  public static void updateAi(EcsEntity entity, Unit unit, float delta) {
    if (unit.locked || unit.ai == null) return;
    unit.ai.update(delta);
    if (entity instanceof MovementAccess movement) {
      movement.movementVelocityXBack(unit.speedX);
      movement.movementVelocityYBack(unit.speedY);
      movement.movementTargetXBack(unit.targetX);
      movement.movementTargetYBack(unit.targetY);
      movement.movementMoving(unit.speedX != 0f || unit.speedY != 0f);
    }
    if (entity instanceof AiControlAccess ai) {
      ai.aiControlState(unit.ai.state.ordinal());
      ai.aiControlThinkTimer(ai.aiControlThinkTimer() + delta);
    }
  }

  public static void updateMovement(EcsEntity entity, Unit unit, float delta) {
    if (!(entity instanceof PositionAccess position)
        || !(entity instanceof MovementAccess movement)) return;
    unit.x = position.positionX();
    unit.y = position.positionY();
    unit.rotation = position.positionRotation();
    unit.speedX = movement.movementVelocityX();
    unit.speedY = movement.movementVelocityY();
    unit.targetX = movement.movementTargetX();
    unit.targetY = movement.movementTargetY();
    unit.speed = movement.movementSpeed();
    if (unit.locked) {
      movement.movementVelocityXBack(0f);
      movement.movementVelocityYBack(0f);
      movement.movementMoving(false);
      return;
    }

    float oldX = unit.x;
    float oldY = unit.y;
    float oldRotation = unit.rotation;
    unit.distToTarget = Mathf.dst(unit.x, unit.y, unit.targetX, unit.targetY);
    if (unit.path == null && unit.distToTarget < 2f) {
      unit.x = unit.targetX;
      unit.y = unit.targetY;
      unit.distToTarget = 0f;
      unit.speedX = 0f;
      unit.speedY = 0f;
    } else {
      unit.x += unit.speedX * delta;
      unit.y += unit.speedY * delta;
    }
    if (unit.distToTarget > 1f) {
      unit.angleToTarget = Angles.angle(unit.x, unit.y, unit.targetX, unit.targetY);
    }
    Entity fixedTarget = unit.mainFixedWeapon == null ? null : unit.mainFixedWeapon.target;
    if (unit.canShoot
        && (unit.ai == null || unit.ai.canTarget())
        && fixedTarget != null
        && fixedTarget.health > 0f) {
      float targetRotation = Angles.angle(unit.x, unit.y, fixedTarget.x, fixedTarget.y) - 90f;
      unit.rotation =
          Angles.moveToward(unit.rotation, targetRotation, unit.rotationSpeed * delta);
    } else if (Mathf.len(unit.speedX, unit.speedY) > 0.01f) {
      unit.rotation =
          Angles.moveToward(unit.rotation, unit.angle - 90f, unit.rotationSpeed * delta);
    }
    unit.type.update(unit, delta);
    unit.moving = unit.x != oldX || unit.y != oldY;
    boolean rotated = !Mathf.equal(unit.rotation, oldRotation);
    if (unit.moving || rotated) unit.refreshEcsHitbox();
    if (unit.moving) unit.velocityDirty = true;

    position.positionXBack(unit.x);
    position.positionYBack(unit.y);
    position.positionRotationBack(unit.rotation);
    movement.movementVelocityXBack(unit.speedX);
    movement.movementVelocityYBack(unit.speedY);
    movement.movementTargetXBack(unit.targetX);
    movement.movementTargetYBack(unit.targetY);
    movement.movementMoving(unit.moving);
  }
}
