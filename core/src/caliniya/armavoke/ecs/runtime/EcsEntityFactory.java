package caliniya.armavoke.ecs.runtime;

import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.Weapon;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.type.type.UnitType;
import caliniya.armavoke.type.type.WeaponType;
import caliniya.armavoke.world.Block;

/** Creates and hydrates generated gameplay entities without legacy model objects. */
public final class EcsEntityFactory {
  private EcsEntityFactory() {}

  public static Unit createUnit(UnitType type, TeamTypes team, float x, float y) {
    if (type == null) throw new IllegalArgumentException("unit type cannot be null");
    Unit unit = (Unit) EcsRuntime.requireWorld().create("unit");
    unit.unitUnitTypeId(type.id);
    unit.x(x);
    unit.y(y);
    unit.rotation(0f);
    unit.health(type.health);
    unit.maxHealth(type.health);
    unit.armor(type.armorMax);
    unit.maxArmor(type.armorMax);
    unit.armorValue(type.armorValue);
    unit.movementSpeed(type.speed / 60f);
    unit.unitSize(type.size);
    unit.collisionWidth(type.hitbox == null ? type.size : type.hitbox[2]);
    unit.collisionHeight(type.hitbox == null ? type.size : type.hitbox[3]);
    unit.collisionSolid(true);
    unit.team(team);
    unit.targetingRange(type.scanDistance);
    unit.targetingScanInterval(15f);
    unit.energyData().max = type.energyMax;
    unit.energyData().current = type.energyMax;
    unit.energyData().regen = type.energyRegen;
    configureInventory(unit, type.itemCap, type.liquidCap, type.powerCap);
    if (type.armorResist != null) {
      System.arraycopy(type.armorResist, 0, unit.combat().armorResist, 0,
          Math.min(type.armorResist.length, unit.combat().armorResist.length));
    }
    populateUnitRuntime(unit, type);
    return unit;
  }

  public static Building createBuilding(Block block, int tx, int ty, int angle, TeamTypes team) {
    if (block == null) throw new IllegalArgumentException("block cannot be null");
    Building building = (Building) EcsRuntime.requireWorld().create("building");
    building.buildingBlockId(block.id);
    building.buildingTileX(tx);
    building.buildingTileY(ty);
    building.buildingAngle(angle);
    building.buildingSize(block.psize);
    float tile = WorldData.tilesize;
    float x = tx * tile + block.psize / 2f;
    float y = ty * tile + block.psize / 2f;
    building.x(x);
    building.y(y);
    building.rotation(angle * 90f);
    building.health(block.health);
    building.maxHealth(block.health);
    building.team(team);
    building.collisionWidth(block.psize);
    building.collisionHeight(block.psize);
    building.collisionSolid(block.solid);
    configureInventory(building, block.capacity, block.liquidCapacity, block.powerCapacity);
    return building;
  }

  public static void hydrate(EcsEntity entity) {
    if (entity instanceof Unit unit) {
      UnitType type = unit.type();
      if (type != null) {
        configureInventory(unit, type.itemCap, type.liquidCap, type.powerCap);
        if (unit.abilities().isEmpty() && unit.weapons().isEmpty()) populateUnitRuntime(unit, type);
      }
    } else if (entity instanceof Building building) {
      Block block = building.block();
      if (block != null) configureInventory(building, block.capacity, block.liquidCapacity, block.powerCapacity);
    }
  }

  private static void populateUnitRuntime(Unit unit, UnitType type) {
    for (Ability ability : type.abilities) unit.addAbility(ability);
    for (WeaponType weaponType : type.weapons) unit.weapons().add(new Weapon(weaponType, unit));
  }

  private static void configureInventory(caliniya.armavoke.base.game.Entity entity,
      int itemCapacity, float liquidCapacity, float powerCapacity) {
    entity.item().capacity = Math.max(0, itemCapacity);
    entity.liquid().capacity = Math.max(0f, liquidCapacity);
    entity.power().powerMax = Math.max(0f, powerCapacity);
  }
}
