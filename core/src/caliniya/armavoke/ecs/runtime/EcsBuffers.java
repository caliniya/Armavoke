package caliniya.armavoke.ecs.runtime;

import caliniya.armavoke.ecs.generated.access.MovementAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;

/** Copies volatile component fields between their simulation and published buffers. */
public final class EcsBuffers {
  private EcsBuffers() {}

  public static void prepare(EcsEntity[] entities) {
    for (EcsEntity entity : entities) {
      if (entity instanceof PositionAccess position) {
        position.positionXBack(position.positionX());
        position.positionYBack(position.positionY());
        position.positionRotationBack(position.positionRotation());
      }
      if (entity instanceof MovementAccess movement) {
        movement.movementVelocityXBack(movement.movementVelocityX());
        movement.movementVelocityYBack(movement.movementVelocityY());
        movement.movementTargetXBack(movement.movementTargetX());
        movement.movementTargetYBack(movement.movementTargetY());
      }
    }
  }

  public static void publish(EcsEntity[] entities) {
    for (EcsEntity entity : entities) {
      if (entity instanceof PositionAccess position) {
        position.positionX(position.positionXBack());
        position.positionY(position.positionYBack());
        position.positionRotation(position.positionRotationBack());
      }
      if (entity instanceof MovementAccess movement) {
        movement.movementVelocityX(movement.movementVelocityXBack());
        movement.movementVelocityY(movement.movementVelocityYBack());
        movement.movementTargetX(movement.movementTargetXBack());
        movement.movementTargetY(movement.movementTargetYBack());
      }
    }
  }
}
