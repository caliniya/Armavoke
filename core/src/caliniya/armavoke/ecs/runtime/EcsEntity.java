package caliniya.armavoke.ecs.runtime;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class EcsEntity {
  private static final int serializationVersion = 1;

  private final String entityType;
  private final long componentMask;
  private volatile int id;
  private volatile boolean active;

  protected EcsEntity(String entityType, long componentMask) {
    this.entityType = entityType;
    this.componentMask = componentMask;
  }

  public final String entityType() {
    return entityType;
  }

  public final long componentMask() {
    return componentMask;
  }

  public final boolean has(long requiredMask) {
    return (componentMask & requiredMask) == requiredMask;
  }

  public final int id() {
    return id;
  }

  public final boolean active() {
    return active;
  }

  final void activate(int assignedId) {
    id = assignedId;
    active = true;
  }

  final void markRemoved() {
    active = false;
  }

  public final void prepareForUse() {
    id = 0;
    active = true;
    resetComponents();
  }

  protected final void releaseForPool() {
    id = 0;
    active = false;
    resetComponents();
  }

  protected final void writeBase(DataOutput output) throws IOException {
    output.writeInt(serializationVersion);
    output.writeInt(id);
  }

  protected final void readBase(DataInput input) throws IOException {
    int version = input.readInt();
    if (version != serializationVersion) {
      throw new IOException("Unsupported ECS entity version: " + version);
    }
    id = input.readInt();
    active = true;
  }

  protected abstract void resetComponents();

  public abstract void write(DataOutput output) throws IOException;

  public abstract void read(DataInput input) throws IOException;
}
