package caliniya.armavoke.ecs.runtime;

import arc.util.Log;
import arc.util.Time;
import caliniya.armavoke.system.System;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class EcsScheduler extends System<EcsScheduler> {
  private final Map<String, ThreadRunner> background = new LinkedHashMap<>();
  private EcsRegistry registry;
  private EcsWorld world;
  private List<EcsRegistry.SystemConfig> mainSystems = List.of();
  private long tick;

  @Override
  public EcsScheduler init() {
    index = 8;
    registry = EcsRegistry.loadGenerated();
    world = new EcsWorld(registry);
    for (EcsRegistry.SystemConfig config : registry.systems()) {
      config.system.initialize(world);
    }
    for (EcsRegistry.ThreadConfig thread : registry.threads()) {
      List<EcsRegistry.SystemConfig> group = registry.systemsForThread(thread.name);
      if (thread.name.equals("main")) {
        mainSystems = group;
      } else {
        background.put(thread.name, new ThreadRunner(thread, group));
      }
    }
    return super.init(false, true);
  }

  @Override
  public void update() {
    if (!inited || paused || world == null) return;
    float delta = Math.min(Time.delta, 4f);
    long currentTick = ++tick;
    for (EcsRegistry.SystemConfig config : mainSystems) {
      if (currentTick % config.interval == 0L) execute(config, delta);
    }
    for (ThreadRunner runner : background.values()) runner.schedule(currentTick, delta);
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
    ThreadRunner runner = background.get(group);
    return runner == null ? 0f : runner.smoothedTps;
  }

  @Override
  public void dispose() {
    for (ThreadRunner runner : background.values()) runner.close();
    background.clear();
    if (registry != null && world != null) {
      for (EcsRegistry.SystemConfig config : registry.systems()) {
        try {
          config.system.dispose(world);
        } catch (Throwable error) {
          Log.err("ECS system dispose failed: @", config.name, error);
        }
      }
      world.clear();
    }
    super.dispose();
  }

  private final class ThreadRunner {
    private final EcsRegistry.ThreadConfig config;
    private final List<EcsRegistry.SystemConfig> systems;
    private final ExecutorService coordinator;
    private final ExecutorService workers;
    private final AtomicBoolean busy = new AtomicBoolean();
    private long tpsWindow = java.lang.System.nanoTime();
    private int completedTicks;
    private float smoothedTps;

    ThreadRunner(
        EcsRegistry.ThreadConfig config, List<EcsRegistry.SystemConfig> systems) {
      this.config = config;
      this.systems = systems;
      coordinator =
          Executors.newSingleThreadExecutor(
              threadFactory("ECS-" + config.name + "-coordinator", config.priority));
      workers =
          Executors.newFixedThreadPool(
              config.workers, threadFactory("ECS-" + config.name, config.priority));
    }

    void schedule(long currentTick, float delta) {
      if (systems.isEmpty() || !busy.compareAndSet(false, true)) return;
      coordinator.execute(
          () -> {
            try {
              List<EcsRegistry.SystemConfig> due = new ArrayList<>();
              for (EcsRegistry.SystemConfig system : systems) {
                if (currentTick % system.interval == 0L) due.add(system);
              }
              for (List<EcsRegistry.SystemConfig> batch : batches(due)) executeBatch(batch, delta);
              updateTps();
            } finally {
              busy.set(false);
            }
          });
    }

    private List<List<EcsRegistry.SystemConfig>> batches(
        List<EcsRegistry.SystemConfig> due) {
      List<List<EcsRegistry.SystemConfig>> result = new ArrayList<>();
      for (EcsRegistry.SystemConfig system : due) {
        if (!system.parallel || result.isEmpty()) {
          result.add(new ArrayList<>(List.of(system)));
          continue;
        }
        List<EcsRegistry.SystemConfig> last = result.get(result.size() - 1);
        boolean compatible = true;
        for (EcsRegistry.SystemConfig existing : last) {
          if (!existing.parallel
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
      CountDownLatch latch = new CountDownLatch(batch.size());
      for (EcsRegistry.SystemConfig system : batch) {
        workers.execute(
            () -> {
              try {
                execute(system, delta);
              } finally {
                latch.countDown();
              }
            });
      }
      try {
        latch.await();
      } catch (InterruptedException interrupted) {
        if (config.interruptible) Thread.currentThread().interrupt();
      }
    }

    private void updateTps() {
      completedTicks++;
      long now = java.lang.System.nanoTime();
      if (now - tpsWindow >= 1_000_000_000L) {
        float measured = completedTicks;
        smoothedTps = smoothedTps == 0f ? measured : smoothedTps * 0.8f + measured * 0.2f;
        completedTicks = 0;
        tpsWindow = now;
      }
    }

    void close() {
      if (config.interruptible) {
        coordinator.shutdownNow();
        workers.shutdownNow();
      } else {
        coordinator.shutdown();
        workers.shutdown();
      }
      try {
        coordinator.awaitTermination(100, TimeUnit.MILLISECONDS);
        workers.awaitTermination(100, TimeUnit.MILLISECONDS);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static boolean contains(String[] values, String target) {
    for (String value : values) if (value.equals(target)) return true;
    return false;
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
