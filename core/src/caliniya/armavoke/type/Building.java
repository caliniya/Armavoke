package caliniya.armavoke.type;

import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.ecs.generated.access.BuildingAccess;
import caliniya.armavoke.ecs.generated.access.ConsumptionAccess;
import caliniya.armavoke.ecs.generated.access.ProductionAccess;
import caliniya.armavoke.ecs.generated.access.SpawnerAccess;
import caliniya.armavoke.ecs.generated.access.TargetingAccess;
import caliniya.armavoke.ecs.generated.access.WeaponAccess;
import caliniya.armavoke.ecs.runtime.EcsRuntime;
import caliniya.armavoke.game.Contents;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.world.Block;
import arc.func.Intc2;

/** Building behavior backed exclusively by ECS components. */
public interface Building extends Entity, WeaponAccess, BuildingAccess, TargetingAccess,
    ProductionAccess, ConsumptionAccess, SpawnerAccess {

  default Block block() { return Contents.getByID(CType.Block, buildingBlockId()); }
  default float size() { return buildingSize(); }
  default int tx() { return buildingTileX(); }
  default int ty() { return buildingTileY(); }
  default int angle() { return buildingAngle(); }
  default void angle(int value) { buildingAngle(value); rotation(value * 90f); }

  default void getOccupiedCoords(Intc2 consumer) {
    Block value = block();
    if (value == null || consumer == null) return;
    if (value.shapeOffsets != null && value.shapeOffsets.length >= 2) {
      int[] offsets = Block.getRotatedOffsets(angle(), value.shapeOffsets);
      for (int i = 0; i + 1 < offsets.length; i += 2) consumer.get(tx() + offsets[i], ty() + offsets[i + 1]);
      return;
    }
    for (int ox = 0; ox < value.size; ox++) for (int oy = 0; oy < value.size; oy++) consumer.get(tx() + ox, ty() + oy);
  }

  default Entity target() {
    Object value = EcsRuntime.find(targetingTargetId());
    return value instanceof Entity entity ? entity : null;
  }

  default void target(Entity value) { targetingTargetId(value == null ? -1 : value.id()); }

  default void draw() {
    Block value = block();
    if (value != null) value.draw(this);
    drawAbilities();
  }

  @Override
  default void remove() {
    if (WorldData.world != null) WorldData.world.removeBuilding(this);
    EcsRuntime.remove(this);
  }
}
