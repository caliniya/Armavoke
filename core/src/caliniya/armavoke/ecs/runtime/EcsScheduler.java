package caliniya.armavoke.ecs.runtime;

import arc.util.Log;
import arc.util.Time;
import caliniya.armavoke.system.System;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic ECS scheduler with thread-group dispatch and frame barriers. */
public final class EcsScheduler extends System<EcsScheduler> {
  private final Map<String, GroupRunner> groups = new LinkedHashMap<>();
  private EcsRegistry registry;
  private EcsWorld world;
  private List<EcsRegistry.SystemConfig> orderedSystems = List.of();
  private long tick;

  @Override
  public EcsScheduler init() {
    if (inited) return this;
    index = 8;
    registry = EcsRegistry.loadGenerated();
    world = new EcsWorld(registry);
    GameEcsBridge.attach(world);
    orderedSystems = registry.orderedSystems();
    for (EcsRegistry.SystemConfig config : orderedSystems) config.system.initialize(world);
    for (EcsRegistry.ThreadConfig thread : registry.threads()) {
      if (!thread.name.equals("main")) groups.put(thread.name, new GroupRunner(thread));
    }
    return super.init(false, true);
  }

  @Override
  public void update() {
    update(Math.min(Time.delta, 4f));
  }

  @Override
  public void update(float delta) {
    if (!inited || paused || world == null) return;
    long currentTick = ++tick;
    GameEcsBridge.beginFrame(world);
    List<EcsRegistry.SystemConfig> due = new ArrayList<>();
    for (EcsRegistry.SystemConfig config : orderedSystems) {
      if (currentTick % config.interval == 0L) due.add(config);
    }
    for (List<EcsRegistry.SystemConfig> batch : batches(due)) {
      EcsBuffers.prepare(world.snapshot());
      executeBatch(batch, delta);
      EcsBuffers.publish(world.snapshot());
      GameEcsBridge.endBatch();
    }
  }

  private List<List<EcsRegistry.SystemConfig>> batches(List<EcsRegistry.SystemConfig> systems) {
    List<List<EcsRegistry.SystemConfig>> result = new ArrayList<>();
    for (EcsRegistry.SystemConfig system : systems) {
      if (system.thread.equals("main") || !system.parallel || result.isEmpty()) {
        result.add(new ArrayList<>(List.of(system)));
        continue;
      }
      List<EcsRegistry.SystemConfig> last = result.get(result.size() - 1);
      boolean compatible = true;
      for (EcsRegistry.SystemConfig existing : last) {
        if (existing.thread.equals("main")
            || !existing.parallel
            || existing.conflicts(system)
            || contains(system.after, existing.name)
            || contains(existing.after, system.name)) {
          compatible = false;
          break;
        }
      }
      if (compatible) last.add(system);
      else result.add(new ArrayList<>(List.of(system)));
    }
    return result;
  }

  private void executeBatch(List<EcsRegistry.SystemConfig> batch, float delta) {
    List<Future<?>> futures = new ArrayList<>();
    for (EcsRegistry.SystemConfig config : batch) {
      if (config.thread.equals("main")) {
        execute(config, delta);
      } else {
        GroupRunner runner = groups.get(config.thread);
        if (runner == null) {
          Log.err("Missing ECS thread group: @", config.thread);
          continue;
        }
        futures.add(runner.submit(() -> execute(config, delta)));
      }
    }
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        return;
      } catch (Throwable error) {
        Log.err("ECS worker failed", error);
      }
    }
  }

  private void execute(EcsRegistry.SystemConfig config, float delta) {
    try {
      config.system.update(world, delta);
      registry.updateComponents(config.name, world.snapshot(), delta);
    } catch (Throwable error) {
      Log.err("ECS system failed: @", config.name, error);
    }
  }

  public EcsWorld world() {
    return world;
  }

  public EcsRegistry registry() {
    return registry;
  }

  public float groupTps(String group) {
    GroupRunner runner = groups.get(group);
    return runner == null ? 0f : runner.smoothedTps;
  }

  @Override
  public void dispose() {
    for (GroupRunner runner : groups.values()) runner.close();
    groups.clear();
    if (registry != null && world != null) {
      for (EcsRegistry.SystemConfig config : orderedSystems) {
        try {
          config.system.dispose(world);
        } catch (Throwable error) {
          Log.err("ECS system dispose failed: @", config.name, error);
        }
      }
      GameEcsBridge.detach(world);
      world.clear();
    }
    super.dispose();
  }

  private static boolean contains(String[] values, String target) {
    for (String value : values) if (value.equals(target)) return true;
    return false;
  }

  private static final class GroupRunner {
    private final EcsRegistry.ThreadConfig config;
    private final ExecutorService executor;
    private long window = java.lang.System.nanoTime();
    private int completions;
    private volatile float smoothedTps;

    GroupRunner(EcsRegistry.ThreadConfig config) {
      this.config = config;
      executor =
          Executors.newFixedThreadPool(
              config.workers, threadFactory("ECS-" + config.name, config.priority));
    }

    Future<?> submit(Runnable runnable) {
      return executor.submit(
          () -> {
            try {
              runnable.run();
            } finally {
              updateTps();
            }
          });
    }

    private synchronized void updateTps() {
      completions++;
      long now = java.lang.System.nanoTime();
      if (now - window >= 1_000_000_000L) {
        float measured = completions;
        smoothedTps = smoothedTps == 0f ? measured : smoothedTps * 0.8f + measured * 0.2f;
        completions = 0;
        window = now;
      }
    }

    void close() {
      if (config.interruptible) executor.shutdownNow();
      else executor.shutdown();
      try {
        executor.awaitTermination(200, TimeUnit.MILLISECONDS);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static ThreadFactory threadFactory(String prefix, int priority) {
    AtomicInteger ids = new AtomicInteger();
    return runnable -> {
      Thread thread = new Thread(runnable, prefix + '-' + ids.incrementAndGet());
      thread.setDaemon(true);
      thread.setPriority(Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, priority)));
      return thread;
    };
  }
}
