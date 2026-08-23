package caliniya.armavoke.ecs.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EcsRegistry {
  public static final String generatedRegistry =
      "caliniya.armavoke.ecs.generated.GeneratedEcsRegistry";

  private final Map<String, ThreadConfig> threads = new LinkedHashMap<>();
  private final Map<String, ComponentConfig> components = new LinkedHashMap<>();
  private final Map<String, SystemConfig> systems = new LinkedHashMap<>();
  private final Map<String, EntityConfig> entities = new LinkedHashMap<>();
  private final Map<String, List<SystemConfig>> systemsByThread = new LinkedHashMap<>();
  private boolean frozen;

  public static EcsRegistry loadGenerated() {
    EcsRegistry registry = new EcsRegistry();
    try {
      Class<?> generated = Class.forName(generatedRegistry);
      generated.getMethod("register", EcsRegistry.class).invoke(null, registry);
      if (!registry.frozen) registry.freeze();
      return registry;
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Generated ECS registry is unavailable", exception);
    }
  }

  public synchronized void registerThread(ThreadConfig config) {
    ensureMutable();
    if (threads.putIfAbsent(config.name, config) != null) {
      throw new IllegalStateException("Duplicate ECS thread: " + config.name);
    }
  }

  public synchronized void registerComponent(ComponentConfig config) {
    ensureMutable();
    if (components.putIfAbsent(config.name, config) != null) {
      throw new IllegalStateException("Duplicate ECS component: " + config.name);
    }
  }

  public synchronized void registerSystem(SystemConfig config) {
    ensureMutable();
    if (systems.putIfAbsent(config.name, config) != null) {
      throw new IllegalStateException("Duplicate ECS system: " + config.name);
    }
  }

  public synchronized void registerEntity(EntityConfig config) {
    ensureMutable();
    if (entities.putIfAbsent(config.name, config) != null) {
      throw new IllegalStateException("Duplicate ECS entity: " + config.name);
    }
  }

  public synchronized void freeze() {
    if (frozen) return;
    for (SystemConfig system : systems.values()) {
      if (!threads.containsKey(system.thread)) {
        throw new IllegalStateException(
            "System " + system.name + " references unknown thread " + system.thread);
      }
      systemsByThread.computeIfAbsent(system.thread, ignored -> new ArrayList<>()).add(system);
    }
    for (Map.Entry<String, List<SystemConfig>> entry : systemsByThread.entrySet()) {
      entry.setValue(Collections.unmodifiableList(sortSystems(entry.getValue())));
    }
    frozen = true;
  }

  private List<SystemConfig> sortSystems(List<SystemConfig> input) {
    List<SystemConfig> sortedByPriority = new ArrayList<>(input);
    sortedByPriority.sort(
        Comparator.comparingInt((SystemConfig config) -> config.priority)
            .thenComparing(config -> config.name));
    Map<String, SystemConfig> local = new HashMap<>();
    for (SystemConfig config : sortedByPriority) local.put(config.name, config);
    List<SystemConfig> result = new ArrayList<>();
    Set<String> visiting = new HashSet<>();
    Set<String> visited = new HashSet<>();
    for (SystemConfig config : sortedByPriority) {
      visit(config, local, visiting, visited, result);
    }
    return result;
  }

  private void visit(
      SystemConfig config,
      Map<String, SystemConfig> local,
      Set<String> visiting,
      Set<String> visited,
      List<SystemConfig> output) {
    if (!visited.add(config.name)) return;
    if (!visiting.add(config.name)) {
      throw new IllegalStateException("Circular ECS system dependency: " + config.name);
    }
    for (String dependency : config.after) {
      SystemConfig localDependency = local.get(dependency);
      if (localDependency != null) visit(localDependency, local, visiting, visited, output);
    }
    visiting.remove(config.name);
    output.add(config);
  }

  public Collection<ThreadConfig> threads() {
    return Collections.unmodifiableCollection(threads.values());
  }

  public Collection<SystemConfig> systems() {
    return Collections.unmodifiableCollection(systems.values());
  }

  public Collection<ComponentConfig> components() {
    return Collections.unmodifiableCollection(components.values());
  }

  public Collection<EntityConfig> entities() {
    return Collections.unmodifiableCollection(entities.values());
  }

  public List<SystemConfig> systemsForThread(String thread) {
    return systemsByThread.getOrDefault(thread, List.of());
  }

  public EntityConfig entity(String name) {
    return entities.get(name);
  }

  public void initialize(EcsEntity entity, EcsWorld world) {
    for (ComponentConfig component : components.values()) {
      if (component.initialize != null && entity.has(1L << component.index)) {
        component.initialize.invoke(entity, 0f);
      }
    }
  }

  public void destroy(EcsEntity entity, EcsWorld world) {
    for (ComponentConfig component : components.values()) {
      if (component.destroy != null && entity.has(1L << component.index)) {
        component.destroy.invoke(entity, 0f);
      }
    }
  }

  public void updateComponents(
      String systemName, EcsEntity[] snapshot, float delta) {
    for (ComponentConfig component : components.values()) {
      if (component.update == null || !component.updateBy.equals(systemName)) continue;
      long mask = 1L << component.index;
      for (EcsEntity entity : snapshot) {
        if (entity.active() && entity.has(mask)) component.update.invoke(entity, delta);
      }
    }
  }

  private void ensureMutable() {
    if (frozen) throw new IllegalStateException("ECS registry is frozen");
  }

  @FunctionalInterface
  public interface ComponentInvoker {
    void invoke(EcsEntity entity, float delta);
  }

  public static final class ThreadConfig {
    public final String name;
    public final int workers;
    public final int priority;
    public final boolean interruptible;

    public ThreadConfig(String name, int workers, int priority, boolean interruptible) {
      this.name = name;
      this.workers = workers;
      this.priority = priority;
      this.interruptible = interruptible;
    }
  }

  public static final class FieldConfig {
    public final String name;
    public final String type;
    public final boolean volatileField;
    public final boolean readonly;
    public final boolean persisted;
    public final String defaultValue;

    public FieldConfig(
        String name,
        String type,
        boolean volatileField,
        boolean readonly,
        boolean persisted,
        String defaultValue) {
      this.name = name;
      this.type = type;
      this.volatileField = volatileField;
      this.readonly = readonly;
      this.persisted = persisted;
      this.defaultValue = defaultValue;
    }
  }

  public static final class ImportConfig {
    public final String componentType;
    public final String[] fields;
    public final String mode;

    public ImportConfig(String componentType, String[] fields, String mode) {
      this.componentType = componentType;
      this.fields = fields;
      this.mode = mode;
    }
  }

  public static final class ComponentConfig {
    public final String name;
    public final String type;
    public final int index;
    public final long requiredMask;
    public final boolean pure;
    public final boolean pooled;
    public final String storage;
    public final String updateBy;
    public final ComponentInvoker update;
    public final ComponentInvoker initialize;
    public final ComponentInvoker destroy;
    public final FieldConfig[] fields;
    public final ImportConfig[] imports;

    public ComponentConfig(
        String name,
        String type,
        int index,
        long requiredMask,
        boolean pure,
        boolean pooled,
        String storage,
        String updateBy,
        ComponentInvoker update,
        ComponentInvoker initialize,
        ComponentInvoker destroy,
        FieldConfig[] fields,
        ImportConfig[] imports) {
      this.name = name;
      this.type = type;
      this.index = index;
      this.requiredMask = requiredMask;
      this.pure = pure;
      this.pooled = pooled;
      this.storage = storage;
      this.updateBy = updateBy;
      this.update = update;
      this.initialize = initialize;
      this.destroy = destroy;
      this.fields = fields;
      this.imports = imports;
    }
  }

  public static final class SystemConfig {
    public final String name;
    public final String thread;
    public final int priority;
    public final long readMask;
    public final long writeMask;
    public final boolean parallel;
    public final int interval;
    public final String[] after;
    public final EcsSystem system;

    public SystemConfig(
        String name,
        String thread,
        int priority,
        long readMask,
        long writeMask,
        boolean parallel,
        int interval,
        String[] after,
        EcsSystem system) {
      this.name = name;
      this.thread = thread;
      this.priority = priority;
      this.readMask = readMask;
      this.writeMask = writeMask;
      this.parallel = parallel;
      this.interval = Math.max(1, interval);
      this.after = after;
      this.system = system;
    }

    public boolean conflicts(SystemConfig other) {
      return (writeMask & (other.readMask | other.writeMask)) != 0L
          || (other.writeMask & readMask) != 0L;
    }
  }

  public static final class EntityConfig {
    public final String name;
    public final String type;
    public final long componentMask;
    public final boolean pooled;
    public final boolean serializable;
    public final String constructor;
    public final String[] abilities;
    public final String[] modules;
    public final Supplier<? extends EcsEntity> factory;
    public final Consumer<EcsEntity> releaser;

    public EntityConfig(
        String name,
        String type,
        long componentMask,
        boolean pooled,
        boolean serializable,
        String constructor,
        String[] abilities,
        String[] modules,
        Supplier<? extends EcsEntity> factory,
        Consumer<EcsEntity> releaser) {
      this.name = name;
      this.type = type;
      this.componentMask = componentMask;
      this.pooled = pooled;
      this.serializable = serializable;
      this.constructor = constructor;
      this.abilities = abilities;
      this.modules = modules;
      this.factory = factory;
      this.releaser = releaser;
    }
  }
}
