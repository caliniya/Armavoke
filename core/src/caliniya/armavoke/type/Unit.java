package caliniya.armavoke.type;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.ecs.generated.access.AiControlAccess;
import caliniya.armavoke.ecs.generated.access.MovementAccess;
import caliniya.armavoke.ecs.generated.access.PathfindingAccess;
import caliniya.armavoke.ecs.generated.access.TargetingAccess;
import caliniya.armavoke.ecs.generated.access.UnitAccess;
import caliniya.armavoke.ecs.generated.access.WeaponAccess;
import caliniya.armavoke.ecs.runtime.EcsRuntime;
import caliniya.armavoke.game.Contents;
import caliniya.armavoke.type.type.UnitType;

/** Unit behavior backed exclusively by ECS components. */
public interface Unit extends Entity, MovementAccess, WeaponAccess, UnitAccess, TargetingAccess,
    AiControlAccess, PathfindingAccess {

  default UnitType type() { return Contents.getByID(CType.Unit, unitUnitTypeId()); }
  default float size() { return unitSize(); }
  default void size(float value) { unitSize(value); }
  default boolean selected() { return unitSelected(); }
  default void selected(boolean value) { unitSelected(value); }
  default Ar<Weapon> weapons() { return runtime().weapons; }

  default Entity target() {
    Object value = EcsRuntime.find(targetingTargetId());
    return value instanceof Entity entity ? entity : null;
  }

  default void target(Entity value) { targetingTargetId(value == null ? -1 : value.id()); }

  default void moveTo(float x, float y) {
    movementTargetX(x);
    movementTargetY(y);
    pathfindingTargetX(x);
    pathfindingTargetY(y);
    pathfindingRepath(true);
    movementMoving(true);
  }

  default void stop() {
    movementMoving(false);
    movementVelocityX(0f);
    movementVelocityY(0f);
    runtime().path.clear();
    runtime().pathIndex = 0;
  }

  default void draw() {
    UnitType value = type();
    if (value != null) value.draw(this);
    drawAbilities();
  }
}
