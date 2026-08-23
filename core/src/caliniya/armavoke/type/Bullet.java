package caliniya.armavoke.type;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.generated.access.BulletAccess;
import caliniya.armavoke.ecs.generated.access.CollisionAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;
import caliniya.armavoke.ecs.generated.access.TeamAccess;
import caliniya.armavoke.ecs.runtime.EcsEntity;
import caliniya.armavoke.ecs.runtime.EcsBulletRuntime;
import caliniya.armavoke.ecs.runtime.EcsRuntime;
import caliniya.armavoke.type.type.BulletType;

/** Bullet behavior backed exclusively by ECS components. */
public interface Bullet extends PositionAccess, BulletAccess, TeamAccess, CollisionAccess {
  default EcsEntity ecs() { return (EcsEntity) this; }
  default int id() { return ecs().id(); }
  default boolean active() { return ecs().active(); }
  default float x() { return positionX(); }
  default float y() { return positionY(); }
  default float rotation() { return positionRotation(); }
  default BulletType type() { return EcsBulletRuntime.type(bulletBulletTypeId()); }
  default Entity owner() {
    Object value = EcsRuntime.find(bulletOwnerId());
    return value instanceof Entity entity ? entity : null;
  }
  default TeamTypes team() {
    int value = teamTeamId();
    TeamTypes[] values = TeamTypes.values();
    return value >= 0 && value < values.length ? values[value] : null;
  }
  default void draw() { if (type() != null) type().draw(this); }
  default void remove() { EcsRuntime.remove(this); }
}
