package caliniya.armavoke.ecs.runtime;

import caliniya.armavoke.ecs.generated.access.EffectAccess;
import caliniya.armavoke.ecs.generated.access.PositionAccess;

/** Access to the single authoritative gameplay ECS world. */
public final class EcsRuntime {
  private static volatile EcsWorld world;

  private EcsRuntime() {}

  public static void attach(EcsWorld value) { world = value; }
  public static void detach(EcsWorld value) { if (world == value) world = null; }
  public static EcsWorld world() { return world; }

  public static EcsWorld requireWorld() {
    EcsWorld value = world;
    if (value == null) throw new IllegalStateException("ECS world is not initialized");
    return value;
  }

  public static EcsEntity find(int id) {
    EcsWorld value = world;
    return value == null ? null : value.find(id);
  }

  public static boolean remove(Object value) {
    EcsWorld current = world;
    return current != null && value instanceof EcsEntity entity && current.remove(entity);
  }

  public static void clear() {
    EcsWorld current = world;
    if (current != null) current.clear();
  }

  public static void clearEffects() {
    EcsWorld current = world;
    if (current == null) return;
    for (EcsEntity entity : current.snapshot()) {
      if ("effect".equals(entity.entityType())) current.remove(entity);
    }
  }

  public static EcsEntity emitEffect(float x, float y, float rotation, float lifetime) {
    EcsEntity entity = requireWorld().create("effect");
    PositionAccess position = (PositionAccess) entity;
    EffectAccess effect = (EffectAccess) entity;
    position.positionX(x);
    position.positionXBack(x);
    position.positionY(y);
    position.positionYBack(y);
    position.positionRotation(rotation);
    position.positionRotationBack(rotation);
    effect.effectLifetime(lifetime);
    return entity;
  }
}
