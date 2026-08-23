package caliniya.armavoke.ecs.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class EcsWorld {
  private static final int serializationVersion = 1;
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

  public synchronized void write(DataOutput output) throws IOException {
    output.writeInt(serializationVersion);
    int count = 0;
    for (EcsEntity entity : snapshot) {
      EcsRegistry.EntityConfig config = registry.entity(entity.entityType());
      if (config != null && config.serializable && entity.active()) count++;
    }
    output.writeInt(count);
    for (EcsEntity entity : snapshot) {
      EcsRegistry.EntityConfig config = registry.entity(entity.entityType());
      if (config == null || !config.serializable || !entity.active()) continue;
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
      try (DataOutputStream entityOutput = new DataOutputStream(bytes)) {
        entity.write(entityOutput);
      }
      byte[] payload = bytes.toByteArray();
      output.writeUTF(entity.entityType());
      output.writeInt(payload.length);
      output.write(payload);
    }
  }

  public synchronized void read(DataInput input) throws IOException {
    int version = input.readInt();
    if (version != serializationVersion) {
      throw new IOException("Unsupported ECS world version: " + version);
    }
    int count = input.readInt();
    if (count < 0 || count > 1_000_000) throw new IOException("Invalid ECS entity count: " + count);
    clear();
    for (int i = 0; i < count; i++) {
      String type = input.readUTF();
      int length = input.readInt();
      if (length < 0 || length > 64 * 1024 * 1024) {
        throw new IOException("Invalid ECS entity payload: " + length);
      }
      byte[] payload = new byte[length];
      input.readFully(payload);
      EcsRegistry.EntityConfig config = registry.entity(type);
      if (config == null || !config.serializable) continue;
      EcsEntity entity = config.factory.get();
      try (DataInputStream entityInput =
          new DataInputStream(new ByteArrayInputStream(payload))) {
        entity.read(entityInput);
      }
      add(entity);
    }
    EcsBuffers.prepare(snapshot);
  }

  private void rebuildSnapshot() {
    snapshot = entities.values().toArray(EcsEntity[]::new);
  }
}
