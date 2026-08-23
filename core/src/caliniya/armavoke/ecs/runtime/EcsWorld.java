package caliniya.armavoke.ecs.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class EcsWorld {
  private static final EcsEntity[] empty = new EcsEntity[0];

  private final EcsRegistry registry;
  private final AtomicInteger nextId = new AtomicInteger(1);
  private final Map<Integer, EcsEntity> entities = new LinkedHashMap<>();
  private volatile EcsEntity[] snapshot = empty;

  public EcsWorld(EcsRegistry registry) {
    this.registry = registry;
  }

  public EcsRegistry registry() {
    return registry;
  }

  public synchronized <T extends EcsEntity> T add(T entity) {
    if (entity == null) throw new IllegalArgumentException("entity cannot be null");
    int id = entity.id();
    if (id <= 0 || entities.containsKey(id)) {
      id = nextId.getAndIncrement();
    } else {
      nextId.accumulateAndGet(id + 1, Math::max);
    }
    entity.activate(id);
    entities.put(id, entity);
    rebuildSnapshot();
    registry.initialize(entity, this);
    return entity;
  }

  public synchronized EcsEntity create(String entityType) {
    EcsRegistry.EntityConfig config = registry.entity(entityType);
    if (config == null) throw new IllegalArgumentException("Unknown ECS entity: " + entityType);
    return add(config.factory.get());
  }

  public synchronized boolean remove(EcsEntity entity) {
    if (entity == null || !entity.active() || entities.get(entity.id()) != entity) return false;
    registry.destroy(entity, this);
    entities.remove(entity.id());
    entity.markRemoved();
    rebuildSnapshot();
    EcsRegistry.EntityConfig config = registry.entity(entity.entityType());
    if (config != null) config.releaser.accept(entity);
    return true;
  }

  public synchronized void clear() {
    EcsEntity[] current = snapshot;
    for (EcsEntity entity : current) remove(entity);
    entities.clear();
    snapshot = empty;
  }

  public EcsEntity find(int id) {
    synchronized (this) {
      return entities.get(id);
    }
  }

  public EcsEntity[] snapshot() {
    return snapshot;
  }

  public int size() {
    return snapshot.length;
  }

  private void rebuildSnapshot() {
    snapshot = entities.values().toArray(EcsEntity[]::new);
  }
}
