package caliniya.armavoke.ecs.runtime;

import caliniya.armavoke.base.effect.Effect;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.generated.access.AiControlAccess;
import caliniya.armavoke.ecs.generated.access.ArmorAccess;
import caliniya.armavoke.ecs.generated.access.BuildingAccess;
import caliniya.armavoke.ecs.generated.access.BulletAccess;
import caliniya.armavoke.ecs.generated.access.CollisionAccess;
import caliniya.armavoke.ecs.generated.access.EffectAccess;
import caliniya.armavoke.ecs.generated.access.EnergyAccess;
import caliniya.armavoke.ecs.generated.access.HealthAccess;
import caliniya.armavoke.ecs.generated.access.MovementAccess;
import caliniya.armavoke.ecs.generated.access.PathfindingAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;
import caliniya.armavoke.ecs.generated.access.ProductionAccess;
import caliniya.armavoke.ecs.generated.access.SpawnerAccess;
import caliniya.armavoke.ecs.generated.access.TargetingAccess;
import caliniya.armavoke.ecs.generated.access.TeamAccess;
import caliniya.armavoke.ecs.generated.access.WeaponAccess;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.Weapon;
import caliniya.armavoke.type.ai.UnitAI;
import caliniya.armavoke.world.blocks.produce.unit.FactoryBuild;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Connects content-rich render objects to generated, flattened ECS entities. */
public final class GameEcsBridge {
  private static final Object lock = new Object();
  private static final IdentityHashMap<Object, EcsEntity> byRuntime = new IdentityHashMap<>();
  private static final Map<Integer, Object> byEcsId = new java.util.HashMap<>();
  private static final ConcurrentLinkedQueue<EffectSeed> pendingEffects =
      new ConcurrentLinkedQueue<>();
  private static volatile EcsWorld currentWorld;

  private GameEcsBridge() {}

  public static void attach(EcsWorld world) {
    if (world == null) return;
    synchronized (lock) {
      if (currentWorld == world) return;
      currentWorld = world;
      byRuntime.clear();
      byEcsId.clear();
    }
    EcsPersistence.restorePending(world);
    reconcile(world);
    drainEffects(world);
    syncAllToLegacy();
  }

  public static void detach(EcsWorld world) {
    synchronized (lock) {
      if (currentWorld != world) return;
      byRuntime.clear();
      byEcsId.clear();
      currentWorld = null;
    }
  }

  public static void beginFrame(EcsWorld world) {
    if (currentWorld != world) attach(world);
    reconcile(world);
    drainEffects(world);
    syncAllFromLegacy();
  }

  public static void endBatch() {
    syncAllToLegacy();
    updateSpatialIndexes();
  }

  public static EcsEntity register(Unit unit) {
    return registerRuntime(unit, "unit", unit == null ? 0 : unit.id);
  }

  public static EcsEntity register(Building building) {
    return registerRuntime(building, "building", building == null ? 0 : building.id);
  }

  public static EcsEntity register(Bullet bullet) {
    return registerRuntime(bullet, "bullet", bullet == null ? 0 : bullet.id);
  }

  private static EcsEntity registerRuntime(Object runtime, String type, int legacyId) {
    if (runtime == null) return null;
    EcsWorld world = currentWorld;
    if (world == null) return null;
    EcsEntity entity;
    boolean restored = false;
    synchronized (lock) {
      entity = byRuntime.get(runtime);
      if (entity != null && entity.active()) return entity;
      entity = findUnbound(world, type, legacyId);
      restored = entity != null;
      if (entity == null) entity = world.create(type);
      byRuntime.put(runtime, entity);
      byEcsId.put(entity.id(), runtime);
    }
    if (restored) syncToLegacy(entity, runtime);
    else syncFromLegacy(entity, runtime);
    return entity;
  }

  private static EcsEntity findUnbound(EcsWorld world, String type, int legacyId) {
    if (legacyId <= 0) return null;
    for (EcsEntity candidate : world.snapshot()) {
      if (!candidate.entityType().equals(type) || byEcsId.containsKey(candidate.id())) continue;
      if (candidate instanceof caliniya.armavoke.ecs.generated.access.UnitAccess unit
          && unit.unitLegacyId() == legacyId) return candidate;
      if (candidate instanceof BuildingAccess building && building.buildingLegacyId() == legacyId) {
        return candidate;
      }
    }
    return null;
  }

  public static void unregister(Object runtime) {
    if (runtime == null) return;
    EcsEntity entity;
    EcsWorld world;
    synchronized (lock) {
      entity = byRuntime.remove(runtime);
      if (entity != null) byEcsId.remove(entity.id());
      world = currentWorld;
    }
    if (entity != null && world != null) world.remove(entity);
  }

  public static Object runtime(EcsEntity entity) {
    if (entity == null) return null;
    synchronized (lock) {
      return byEcsId.get(entity.id());
    }
  }

  public static EcsEntity ecs(Object runtime) {
    synchronized (lock) {
      return byRuntime.get(runtime);
    }
  }

  public static Entity legacyEntity(int ecsId) {
    synchronized (lock) {
      Object value = byEcsId.get(ecsId);
      return value instanceof Entity entity ? entity : null;
    }
  }

  public static int ecsId(Entity entity) {
    synchronized (lock) {
      EcsEntity value = byRuntime.get(entity);
      return value == null ? -1 : value.id();
    }
  }

  public static void reconcile(EcsWorld world) {
    IdentityHashMap<Object, Boolean> live = new IdentityHashMap<>();
    if (WorldData.units != null) WorldData.units.each(unit -> live.put(unit, Boolean.TRUE));
    if (WorldData.buildings != null) {
      WorldData.buildings.each(building -> live.put(building, Boolean.TRUE));
    }
    for (Object value : live.keySet()) {
      if (value instanceof Unit unit) register(unit);
      else if (value instanceof Building building) register(building);
    }

    List<Object> stale = new ArrayList<>();
    synchronized (lock) {
      for (Object value : byRuntime.keySet()) {
        if ((value instanceof Unit || value instanceof Building) && !live.containsKey(value)) {
          stale.add(value);
        }
      }
    }
    for (Object value : stale) unregister(value);
  }

  public static void resetForWorld() {
    EcsWorld world;
    synchronized (lock) {
      world = currentWorld;
      byRuntime.clear();
      byEcsId.clear();
    }
    pendingEffects.clear();
    if (world != null) world.clear();
  }

  public static void effectEmitted(
      int id, Effect effect, float x, float y, float rotation) {
    if (effect == null) return;
    pendingEffects.add(new EffectSeed(id, effect, x, y, rotation));
  }

  private static void drainEffects(EcsWorld world) {
    EffectSeed seed;
    while ((seed = pendingEffects.poll()) != null) {
      EcsEntity entity = world.create("effect");
      PositionAccess position = (PositionAccess) entity;
      position.positionX(seed.x);
      position.positionXBack(seed.x);
      position.positionY(seed.y);
      position.positionYBack(seed.y);
      position.positionRotation(seed.rotation);
      position.positionRotationBack(seed.rotation);
      EffectAccess effect = (EffectAccess) entity;
      effect.effectComponent().effectId = seed.id;
      effect.effectComponent().lifetime = seed.effect.lifetime;
      effect.effectComponent().clip = seed.effect.clip;
    }
  }

  public static void clearEffects() {
    pendingEffects.clear();
    EcsWorld world = currentWorld;
    if (world == null) return;
    for (EcsEntity entity : world.snapshot()) {
      if (entity.entityType().equals("effect")) world.remove(entity);
    }
  }

  public static void syncAllFromLegacy() {
    List<Map.Entry<Object, EcsEntity>> entries;
    synchronized (lock) {
      entries = new ArrayList<>(byRuntime.entrySet());
    }
    for (Map.Entry<Object, EcsEntity> entry : entries) {
      if (entry.getValue().active()) syncFromLegacy(entry.getValue(), entry.getKey());
    }
  }

  public static void syncAllToLegacy() {
    List<Map.Entry<Object, EcsEntity>> entries;
    synchronized (lock) {
      entries = new ArrayList<>(byRuntime.entrySet());
    }
    for (Map.Entry<Object, EcsEntity> entry : entries) {
      if (entry.getValue().active()) syncToLegacy(entry.getValue(), entry.getKey());
    }
    for (Map.Entry<Object, EcsEntity> entry : entries) syncTarget(entry.getValue(), entry.getKey());
  }

  public static void syncFromLegacy(Object runtime) {
    EcsEntity entity = ecs(runtime);
    if (entity != null && entity.active()) syncFromLegacy(entity, runtime);
  }

  private static void syncFromLegacy(EcsEntity entity, Object runtime) {
    if (runtime instanceof Unit unit) syncUnitFromLegacy(entity, unit);
    else if (runtime instanceof Building building) syncBuildingFromLegacy(entity, building);
    else if (runtime instanceof Bullet bullet) syncBulletFromLegacy(entity, bullet);
  }

  private static void syncUnitFromLegacy(EcsEntity entity, Unit unit) {
    PositionAccess position = (PositionAccess) entity;
    setPosition(position, unit.x, unit.y, unit.rotation);
    HealthAccess health = (HealthAccess) entity;
    health.healthHealth(unit.health);
    health.healthMaxHealth(unit.maxHealth);
    ArmorAccess armor = (ArmorAccess) entity;
    armor.armorArmor(unit.armor);
    armor.armorMaxArmor(unit.armorMax);
    armor.armorArmorValue(unit.armorValue);
    MovementAccess movement = (MovementAccess) entity;
    setMovement(movement, unit.speedX, unit.speedY, unit.targetX, unit.targetY);
    movement.movementSpeed(unit.speed);
    movement.movementMoving(unit.moving);
    EnergyAccess energy = (EnergyAccess) entity;
    energy.energyComponent().current = unit.energy;
    energy.energyComponent().max = unit.energyMax;
    energy.energyComponent().regen = unit.energyRegen;
    WeaponAccess weapon = (WeaponAccess) entity;
    Weapon primary = unit.weapons.isEmpty() ? null : unit.weapons.get(0);
    weapon.weaponWeaponTypeId(primary == null ? -1 : 0);
    weapon.weaponReload(primary == null ? 0f : primary.reloadTimer);
    weapon.weaponReloadTime(primary == null ? 0f : primary.type.reload);
    weapon.weaponTargetId(primary == null ? -1 : ecsId(primary.target));
    weapon.weaponRange(primary == null ? unit.type.engageRange : primary.type.range);
    caliniya.armavoke.ecs.generated.access.UnitAccess unitData =
        (caliniya.armavoke.ecs.generated.access.UnitAccess) entity;
    unitData.unitUnitTypeId(contentId(unit.type == null ? null : unit.type.internalName));
    unitData.unitSize(unit.size);
    unitData.unitLegacyId(unit.id);
    TeamAccess team = (TeamAccess) entity;
    team.teamTeamId(unit.team == null ? -1 : unit.team.ordinal());
    TargetingAccess targeting = (TargetingAccess) entity;
    targeting.targetingTargetId(ecsId(unit.target));
    targeting.targetingRange(unit.type == null ? 0f : unit.type.scanDistance);
    targeting.targetingScanTimer(unit.scanCooldown);
    AiControlAccess ai = (AiControlAccess) entity;
    ai.aiControlState(unit.ai == null ? 0 : unit.ai.state.ordinal());
    PathfindingAccess path = (PathfindingAccess) entity;
    path.pathfindingTargetX(unit.targetX);
    path.pathfindingTargetY(unit.targetY);
    path.pathfindingRouteVersion(unit.routeVersion);
    path.pathfindingRepath(!unit.pathed && isNavigating(unit));
    CollisionAccess collision = (CollisionAccess) entity;
    collision.collisionWidth(unit.hitboxSize());
    collision.collisionHeight(unit.hitboxSize());
    collision.collisionSolid(true);
  }

  private static void syncBuildingFromLegacy(EcsEntity entity, Building building) {
    PositionAccess position = (PositionAccess) entity;
    setPosition(position, building.x, building.y, building.rotation);
    HealthAccess health = (HealthAccess) entity;
    health.healthHealth(building.health);
    health.healthMaxHealth(building.maxHealth);
    ArmorAccess armor = (ArmorAccess) entity;
    armor.armorArmor(building.armor);
    armor.armorMaxArmor(building.armorMax);
    armor.armorArmorValue(building.armorValue);
    EnergyAccess energy = (EnergyAccess) entity;
    energy.energyComponent().current = building.energy;
    energy.energyComponent().max = building.energyMax;
    energy.energyComponent().regen = building.energyRegen;
    BuildingAccess data = (BuildingAccess) entity;
    data.buildingBlockId(contentId(building.block == null ? null : building.block.internalName));
    data.buildingSize(building.hitboxSize());
    data.buildingLegacyId(building.id);
    TeamAccess team = (TeamAccess) entity;
    team.teamTeamId(building.team == null ? -1 : building.team.ordinal());
    TargetingAccess targeting = (TargetingAccess) entity;
    targeting.targetingTargetId(ecsId(building.target));
    CollisionAccess collision = (CollisionAccess) entity;
    collision.collisionWidth(building.hitboxSize());
    collision.collisionHeight(building.hitboxSize());
    collision.collisionSolid(building.block != null && building.block.solid);
    if (building instanceof FactoryBuild factoryBuild) {
      EcsFactoryRuntime.writeViewToComponents(entity, factoryBuild);
    }
  }

  private static void syncBulletFromLegacy(EcsEntity entity, Bullet bullet) {
    PositionAccess position = (PositionAccess) entity;
    setPosition(position, bullet.x, bullet.y, bullet.rotation);
    BulletAccess data = (BulletAccess) entity;
    float speed = (float) Math.sqrt(bullet.velX * bullet.velX + bullet.velY * bullet.velY);
    data.bulletDamage(bullet.type == null ? 0f : bullet.type.damage);
    data.bulletSpeed(speed);
    data.bulletDirectionX(speed == 0f ? 0f : bullet.velX / speed);
    data.bulletDirectionY(speed == 0f ? 0f : bullet.velY / speed);
    data.bulletLifetime(bullet.type == null ? 0f : bullet.type.lifetime);
    data.bulletTime(bullet.time);
    data.bulletLegacyId(bullet.id);
    TeamAccess team = (TeamAccess) entity;
    team.teamTeamId(bullet.team == null ? -1 : bullet.team.ordinal());
    CollisionAccess collision = (CollisionAccess) entity;
    float size = bullet.type == null ? 0f : bullet.type.size;
    collision.collisionWidth(size);
    collision.collisionHeight(size);
    collision.collisionSolid(false);
  }

  private static void syncToLegacy(EcsEntity entity, Object runtime) {
    if (runtime instanceof Unit unit) {
      PositionAccess position = (PositionAccess) entity;
      float oldX = unit.x;
      float oldY = unit.y;
      unit.x = position.positionX();
      unit.y = position.positionY();
      unit.rotation = position.positionRotation();
      if (oldX != unit.x || oldY != unit.y) unit.velocityDirty = true;
      HealthAccess health = (HealthAccess) entity;
      unit.health = health.healthHealth();
      unit.maxHealth = health.healthMaxHealth();
      ArmorAccess armor = (ArmorAccess) entity;
      unit.armor = armor.armorArmor();
      unit.armorMax = armor.armorMaxArmor();
      unit.armorValue = armor.armorArmorValue();
      MovementAccess movement = (MovementAccess) entity;
      unit.speedX = movement.movementVelocityX();
      unit.speedY = movement.movementVelocityY();
      unit.targetX = movement.movementTargetX();
      unit.targetY = movement.movementTargetY();
      unit.speed = movement.movementSpeed();
      unit.moving = movement.movementMoving();
      EnergyAccess energy = (EnergyAccess) entity;
      unit.energy = energy.energyComponent().current;
      unit.energyMax = energy.energyComponent().max;
      unit.energyRegen = energy.energyComponent().regen;
      applyTeam(unit, ((TeamAccess) entity).teamTeamId());
      AiControlAccess ai = (AiControlAccess) entity;
      if (unit.ai != null && ai.aiControlState() >= 0
          && ai.aiControlState() < UnitAI.State.values().length) {
        unit.ai.state = UnitAI.State.values()[ai.aiControlState()];
      }
      PathfindingAccess path = (PathfindingAccess) entity;
      unit.routeVersion = path.pathfindingRouteVersion();
      if (path.pathfindingRepath()) unit.pathed = false;
    } else if (runtime instanceof Building building) {
      PositionAccess position = (PositionAccess) entity;
      building.x = position.positionX();
      building.y = position.positionY();
      building.rotation = position.positionRotation();
      HealthAccess health = (HealthAccess) entity;
      building.health = health.healthHealth();
      building.maxHealth = health.healthMaxHealth();
      ArmorAccess armor = (ArmorAccess) entity;
      building.armor = armor.armorArmor();
      building.armorMax = armor.armorMaxArmor();
      building.armorValue = armor.armorArmorValue();
      EnergyAccess energy = (EnergyAccess) entity;
      building.energy = energy.energyComponent().current;
      building.energyMax = energy.energyComponent().max;
      building.energyRegen = energy.energyComponent().regen;
      applyTeam(building, ((TeamAccess) entity).teamTeamId());
      if (building instanceof FactoryBuild factoryBuild) {
        EcsFactoryRuntime.readComponentsToView(entity, factoryBuild);
      }
    } else if (runtime instanceof Bullet bullet) {
      PositionAccess position = (PositionAccess) entity;
      bullet.x = position.positionX();
      bullet.y = position.positionY();
      bullet.rotation = position.positionRotation();
      BulletAccess data = (BulletAccess) entity;
      bullet.velX = data.bulletDirectionX() * data.bulletSpeed();
      bullet.velY = data.bulletDirectionY() * data.bulletSpeed();
      bullet.time = data.bulletTime();
    }
  }

  private static void syncTarget(EcsEntity entity, Object runtime) {
    if (!(entity instanceof TargetingAccess targeting)) return;
    Entity target = legacyEntity(targeting.targetingTargetId());
    if (runtime instanceof Unit unit) unit.target = target;
    else if (runtime instanceof Building building) building.target = target;
  }

  private static void applyTeam(Entity entity, int teamId) {
    if (teamId < 0 || teamId >= TeamTypes.values().length) return;
    TeamTypes team = TeamTypes.values()[teamId];
    entity.team = team;
    entity.teamData = team.data();
  }

  private static void setPosition(PositionAccess position, float x, float y, float rotation) {
    position.positionX(x);
    position.positionXBack(x);
    position.positionY(y);
    position.positionYBack(y);
    position.positionRotation(rotation);
    position.positionRotationBack(rotation);
  }

  private static void setMovement(
      MovementAccess movement, float vx, float vy, float targetX, float targetY) {
    movement.movementVelocityX(vx);
    movement.movementVelocityXBack(vx);
    movement.movementVelocityY(vy);
    movement.movementVelocityYBack(vy);
    movement.movementTargetX(targetX);
    movement.movementTargetXBack(targetX);
    movement.movementTargetY(targetY);
    movement.movementTargetYBack(targetY);
  }

  private static boolean isNavigating(Unit unit) {
    return WorldData.moveunits != null && WorldData.moveunits.array.contains(unit);
  }

  private static int contentId(String name) {
    return name == null ? -1 : name.hashCode();
  }

  private static void updateSpatialIndexes() {
    if (WorldData.units == null) return;
    List<Unit> moved = new ArrayList<>();
    WorldData.units.each(unit -> {
      if (unit != null && unit.velocityDirty) moved.add(unit);
    });
    for (Unit unit : moved) {
      WorldData.units.move(unit, unit.x, unit.y);
      unit.velocityDirty = false;
    }
  }

  private static final class EffectSeed {
    final int id;
    final Effect effect;
    final float x;
    final float y;
    final float rotation;

    EffectSeed(int id, Effect effect, float x, float y, float rotation) {
      this.id = id;
      this.effect = effect;
      this.x = x;
      this.y = y;
      this.rotation = rotation;
    }
  }
}
