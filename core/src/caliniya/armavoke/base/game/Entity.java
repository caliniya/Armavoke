package caliniya.armavoke.base.game;

import arc.math.Mathf;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.ecs.definition.GameComponents;
import caliniya.armavoke.ecs.generated.access.ArmorAccess;
import caliniya.armavoke.ecs.generated.access.CollisionAccess;
import caliniya.armavoke.ecs.generated.access.CombatAccess;
import caliniya.armavoke.ecs.generated.access.EnergyAccess;
import caliniya.armavoke.ecs.generated.access.HealthAccess;
import caliniya.armavoke.ecs.generated.access.InventoryAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;
import caliniya.armavoke.ecs.generated.access.RuntimeDataAccess;
import caliniya.armavoke.ecs.generated.access.TeamAccess;
import caliniya.armavoke.ecs.runtime.EcsEntity;
import caliniya.armavoke.ecs.runtime.EcsRuntime;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.Enhancement;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.type.enhance.api.Updatable;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.type.module.LiquidModule;
import caliniya.armavoke.type.module.PowerModule;

/** Stateless gameplay contract implemented directly by generated ECS entities. */
public interface Entity extends PositionAccess, HealthAccess, ArmorAccess, EnergyAccess,
    InventoryAccess, RuntimeDataAccess, CombatAccess, TeamAccess, CollisionAccess {

  default EcsEntity ecs() { return (EcsEntity) this; }
  default int id() { return ecs().id(); }
  default boolean active() { return ecs().active(); }

  default float x() { return positionX(); }
  default void x(float value) { positionX(value); positionXBack(value); }
  default float y() { return positionY(); }
  default void y(float value) { positionY(value); positionYBack(value); }
  default float rotation() { return positionRotation(); }
  default void rotation(float value) { positionRotation(value); positionRotationBack(value); }

  default float health() { return healthHealth(); }
  default void health(float value) { healthHealth(value); }
  default float maxHealth() { return healthMaxHealth(); }
  default void maxHealth(float value) { healthMaxHealth(value); }
  default float armor() { return armorArmor(); }
  default void armor(float value) { armorArmor(value); }
  default float maxArmor() { return armorMaxArmor(); }
  default void maxArmor(float value) { armorMaxArmor(value); }
  default float armorValue() { return armorArmorValue(); }
  default void armorValue(float value) { armorArmorValue(value); }

  default GameComponents.Energy energyData() { return energyComponent(); }
  default float energy() { return energyData().current; }
  default void energy(float value) { energyData().current = Mathf.clamp(value, 0f, energyData().max); }
  default float energyMax() { return energyData().max; }
  default void energyMax(float value) { energyData().max = Math.max(0f, value); }

  default GameComponents.Inventory inventory() { return inventoryComponent(); }
  default ItemModule item() { return inventory().items; }
  default LiquidModule liquid() { return inventory().liquids; }
  default PowerModule power() { return inventory().power; }
  default GameComponents.RuntimeData runtime() { return runtimeDataComponent(); }
  default GameComponents.Combat combat() { return combatComponent(); }
  default Ar<Ability> abilities() { return runtime().abilities; }
  default Ar<Enhancement> enhancements() { return runtime().enhancements; }
  default Ar<Updatable> updatables() { return runtime().updatables; }

  default TeamTypes team() {
    int id = teamTeamId();
    TeamTypes[] values = TeamTypes.values();
    return id >= 0 && id < values.length ? values[id] : null;
  }

  default void team(TeamTypes value) { teamTeamId(value == null ? -1 : value.ordinal()); }
  default float width() { return collisionWidth(); }
  default float height() { return collisionHeight(); }

  default float heat() { return combat().heat; }
  default void heat(float value) { combat().heat = Mathf.clamp(value, 0f, combat().heatMax); }
  default float heatMax() { return combat().heatMax; }
  default boolean locked() { return combat().locked; }
  default void locked(boolean value) { combat().locked = value; }

  default float armorResistance(DamageType type) {
    float[] values = combat().armorResist;
    return type == null || values == null || type.ordinal() >= values.length ? 0f : values[type.ordinal()];
  }

  default void armorResistance(DamageType type, float value) {
    if (type != null && combat().armorResist != null) {
      combat().armorResist[type.ordinal()] = Mathf.clamp(value, 0f, 1f);
    }
  }

  default Ability addAbility(Ability prototype) {
    if (prototype == null) return null;
    Ability ability = prototype.copy();
    abilities().add(ability);
    ability.onCreate(this);
    return ability;
  }

  default <T extends Ability> T getAbility(Class<T> type) {
    for (Ability ability : abilities()) if (type.isInstance(ability)) return type.cast(ability);
    return null;
  }

  default void addEnhancement(Enhancement enhancement) {
    if (enhancement != null) enhancements().add(enhancement);
  }

  default void updateBase(float delta) {
    GameComponents.Energy energy = energyData();
    float drain = 0f;
    for (Ability ability : abilities()) drain += Math.max(0f, ability.energyUse());
    energy.current = Mathf.clamp(energy.current + (energy.regen / 60f - drain) * delta, 0f, energy.max);
    if (combat().heatable && combat().heat > 0f) {
      combat().heat = Math.max(0f, combat().heat - combat().heatSpeed * delta / 60f);
    }
    for (Ability ability : abilities()) ability.update(this, delta);
    for (Updatable updatable : updatables()) updatable.update(this, delta);
  }

  default void drawAbilities() {
    for (Ability ability : abilities()) ability.draw(this);
  }

  default void damage(float amount, DamageType type) {
    damage(amount, type, false, false, false, false);
  }

  default void damage(float amount, DamageType type, boolean breakArmor, boolean bypassArmor,
      boolean breakShield, boolean bypassShield) {
    if (amount <= 0f || health() <= 0f) return;
    float remaining = amount;
    for (Ability ability : abilities()) {
      remaining = ability.applyDamage(this, remaining, type, breakShield, bypassShield);
      if (remaining <= 0f) return;
    }
    if (!bypassArmor && armor() > 0f) {
      if (!breakArmor && remaining < armorValue()) return;
      remaining *= 1f - armorResistance(type);
      armor(Math.max(0f, armor() - remaining));
      return;
    }
    health(health() - remaining);
    if (health() <= 0f) kill();
  }

  default void hit(Bullet bullet) {
    if (bullet == null || bullet.type() == null) return;
    damage(bullet.type().damage, bullet.type().damageType, bullet.type().breakArmor,
        bullet.type().bypassArmor, bullet.type().breakShield, bullet.type().bypassShield);
  }

  default void kill() {
    health(0f);
    remove();
  }

  default void remove() { EcsRuntime.remove(this); }
}
