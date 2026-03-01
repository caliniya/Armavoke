package caliniya.armavoke.system;

import arc.Events;
import arc.util.Log;
import arc.util.Threads;
import caliniya.armavoke.base.type.EventType;

/**
 * 抽象系统类，代表游戏或应用中的一个独立逻辑模块。
 * <p>
 * 支持两种运行模式：
 * <ul>
 *     <li>主线程运行：由外部手动调用 {@link #update()} 或 {@link #update(float)}。</li>
 *     <li>独立线程运行：在 {@link #init(boolean)} 中开启独立线程循环，自带帧率控制（默认 60 TPS）和 TPS 统计。</li>
 * </ul>
 * </p>
 *
 * @param <T> 自引用泛型类型，允许子类方法返回自身类型以支持链式调用。
 */
public abstract class System<T extends System<T>> implements Comparable<System<?>> {

  /** 系统是否已初始化的标志，防止重复初始化。 */
  public boolean inited = false;

  /** 系统的执行优先级索引，数值越小优先级越高（用于排序）。 */
  public int index = 0;

  /** 是否在独立线程中运行。 */
  protected boolean isThreaded = false;

  /** 线程运行状态标志，使用 volatile 保证多线程可见性。 */
  private volatile boolean threadRunning = false;
  
  /** 暂停状态标志，使用 volatile 保证多线程可见性。 */
  private volatile boolean paused = false;
  
  /** 暂停锁对象，用于线程的等待/唤醒机制。 */
  private final Object pauseLock = new Object();

  /** 系统运行的线程实例。 */
  private Thread systemThread;

  /** 目标帧时间（纳秒），默认为 16.6ms（约 60 TPS）。 */
  protected long targetNs = 16_666_666L;

  // ========== 公有测量变量 ==========

  /** 实时 TPS（每秒刻数），表示上一秒内的实际更新次数。 */
  public float tps = 0f;

  /** 平滑 TPS，基于过去 60 个采样值的移动平均值。 */
  public float smoothedTps = 0f;

  // ===================================

  /**
   * 使用默认配置初始化系统。
   * <p>默认根据 {@link #isThreaded} 字段决定是否开启线程。</p>
   *
   * @return 当前系统实例（用于链式调用）。
   */
  public T init() {
    return init(this.isThreaded);
  }

  /**
   * 初始化系统。
   * <p>
   * 如果系统未初始化，将设置运行模式，注册游戏暂停事件监听器。
   * 如果指定为线程模式，将启动独立线程。
   * </p>
   *
   * @param runInThread 是否在独立线程中运行。
   * @return 当前系统实例。
   */
  public T init(boolean runInThread) {
    if (inited) return (T) this;

    this.isThreaded = runInThread;
    this.inited = true;

    if (isThreaded) {
      startThread();
      // 注册线程停止事件，当收到 ThreadedStop 事件时停止线程
      Events.run(EventType.events.ThreadedStop, () -> stopThread());
    }
    // 注册游戏暂停事件，同步系统的暂停状态
    Events.on(EventType.GamePause.class, event -> setPaused(event.pause));

    return (T) this;
  }

  /**
   * 系统逻辑更新方法（带时间增量）。
   * <p>子类应重写此方法实现具体逻辑。通常用于线程模式下的时间补偿计算。</p>
   *
   * @param delta 以 60TPS 为基准的帧时间增量。
   *              值为 1.0 表示理想的一帧（约16.6ms），最大限制为 4.0。
   */
  public void update(float delta) {}

  /**
   * 系统逻辑更新方法（无参数）。
   * <p>子类应重写此方法实现具体逻辑。通常用于非线程模式或固定步长逻辑。</p>
   */
  public void update() {}

  /**
   * 销毁系统，释放资源。
   * <p>会停止独立线程（如果正在运行）。</p>
   */
  public void dispose() {
    stopThread();
  }

  /**
   * 设置系统的暂停状态。
   * <p>
   * 如果从暂停恢复，会重置计时器并唤醒等待的线程，防止因暂停导致的时间跳跃。
   * </p>
   *
   * @param paused true 为暂停，false 为恢复。
   */
  public void setPaused(boolean paused) {
    synchronized (pauseLock) {
      this.paused = paused;
      if (!paused) {
        // 恢复时重置最后循环时间，避免计算出一极大的 delta 值
        lastLoopTime = java.lang.System.nanoTime();
        pauseLock.notifyAll();
      }
    }
  }

  /**
   * 获取系统是否处于暂停状态。
   *
   * @return 如果暂停返回 true。
   */
  public boolean isPaused() {
    return paused;
  }

  // ========== 内部计时与统计变量 ==========

  /** 上一次循环的时间戳（纳秒），用于计算帧间隔。 */
  private long lastLoopTime = 0;
  
  /** 计数器，用于统计每秒的 Tick 次数。 */
  private int tickCounter = 0;
  
  /** 上一次 TPS 更新的时间戳（纳秒）。 */
  private long lastTPSUpdate = 0;
  
  /** TPS 采样数组，用于计算平滑 TPS。 */
  private final float[] tpsSamples = new float[60];
  
  /** 当前采样写入索引。 */
  private int tpsIndex = 0;
  
  /** 已填充的采样数量。 */
  private int tpsFilled = 0;

  // =============================

  /**
   * 启动独立系统线程。
   * <p>如果线程已在运行则不执行操作。</p>
   */
  private void startThread() {
    if (threadRunning) return;

    threadRunning = true;

    systemThread =
        Threads.daemon(
            "System-" + this.getClass().getSimpleName(),
            () -> {
              Log.info("System thread started: @", this.getClass().getSimpleName());

              lastLoopTime = java.lang.System.nanoTime();
              lastTPSUpdate = lastLoopTime;

              while (threadRunning) {
                try {
                  // --- 暂停控制 ---
                  synchronized (pauseLock) {
                    // 如果处于暂停状态，线程进入等待，释放锁
                    while (paused && threadRunning) {
                      pauseLock.wait();
                    }
                  }
                  if (!threadRunning) break;

                  // --- 帧时间计算 ---
                  long now = java.lang.System.nanoTime();
                  long elapsedNs = now - lastLoopTime;
                  lastLoopTime = now;

                  // 计算 delta（以目标帧时间 targetNs 为单位）
                  // 限制最大 delta 为 4.0，防止长时间暂停后逻辑爆炸
                  float delta = (float) (elapsedNs / (double) targetNs);
                  if (delta > 4f) delta = 4f;

                  try {
                    // --- 执行逻辑 ---
                    update(delta);
                    update();
                    // 注意：通常子类只会实现其中一个 update 方法
                    tickCounter++;
                  } catch (Exception e) {
                    Log.err(
                        "Error in system thread: @", this.getClass().getSimpleName() + "  " + e);
                  }

                  // --- TPS 统计 ---
                  // 每隔 1 秒统计一次实时 TPS 并更新平滑 TPS
                  if (now - lastTPSUpdate >= 1_000_000_000L) {
                    tps = tickCounter;
                    tickCounter = 0;
                    lastTPSUpdate = now;
                    updateSmoothedTPS(tps);
                  }

                  // --- 帧率控制 ---
                  long endTime = java.lang.System.nanoTime();
                  // 计算剩余时间：目标时间 - 实际消耗时间
                  long sleepNs = targetNs - (endTime - now);

                  if (sleepNs > 0) {
                    // 如果有余量，则休眠
                    Thread.sleep(sleepNs / 1_000_000, (int) (sleepNs % 1_000_000));
                  } else {
                    // 如果超时，让出 CPU 时间片，避免占用过高
                    Thread.yield();
                  }

                } catch (InterruptedException e) {
                  // 捕获中断异常，正常退出循环
                  threadRunning = false;
                  Thread.currentThread().interrupt();
                  Log.err(e);
                } catch (Exception e) {
                  Log.err("Critical error in system loop: @", this.getClass().getSimpleName(), e);
                }
              }
              Log.info("System thread stopped: @", this.getClass().getSimpleName());
            });
  }

  /**
   * 更新平滑 TPS 值。
   * <p>使用循环数组存储最近 60 个采样值进行移动平均计算。</p>
   *
   * @param tps 当前的实时 TPS 值。
   */
  private void updateSmoothedTPS(float tps) {
    tpsSamples[tpsIndex] = tps;
    tpsIndex = (tpsIndex + 1) % tpsSamples.length;
    if (tpsFilled < tpsSamples.length) tpsFilled++;

    float sum = 0f;
    for (int i = 0; i < tpsFilled; i++) {
      sum += tpsSamples[i];
    }
    smoothedTps = sum / tpsFilled;
  }

  /**
   * 停止独立系统线程。
   * <p>会中断线程并等待其结束（最多等待 100ms）。</p>
   */
  private void stopThread() {
    threadRunning = false;
    if (systemThread != null) {
      systemThread.interrupt();
      try {
        systemThread.join(100);
      } catch (Exception ignored) {
      }
      systemThread = null;
    }
  }

  /**
   * 比较两个系统的优先级。
   * <p>基于 {@link #index} 进行比较，用于对系统列表进行排序。</p>
   *
   * @param other 要比较的另一个系统。
   * @return 比较结果。
   */
  @Override
  public int compareTo(System<?> other) {
    return Integer.compare(this.index, other.index);
  }
}