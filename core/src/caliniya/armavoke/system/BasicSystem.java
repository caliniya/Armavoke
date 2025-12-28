package caliniya.armavoke.system;

import arc.Events;
import arc.util.Log;
import arc.util.Threads;
import caliniya.armavoke.base.type.EventType;

public abstract class BasicSystem<T extends BasicSystem<T>> implements Comparable<BasicSystem<?>> {

  public boolean inited = false;
  public int index = 0;

  protected boolean isThreaded = false;

  private volatile boolean threadRunning = false;

  private volatile boolean paused = false;
  private final Object pauseLock = new Object(); // 用于线程同步锁

  private Thread systemThread;
  protected long threadSleepMs = 16;

  public float delta = 1f;
  private static final double NS_PER_TICK = 1_000_000_000.0 / 60.0;
  private static final float MAX_DELTA = 4.0f;

  private long lastTime;
  private long now;
  private long elapsedNs;
  private long logicStartNs;
  private long logicDurationMs;
  private long sleepTimeMs;

  /** 普通初始化 */
  public T init() {
    return init(this.isThreaded);
  }

  /** 带参数初始化 */
  public T init(boolean runInThread) {
    if (inited) return (T) this;

    this.isThreaded = runInThread;
    this.inited = true;

    if (isThreaded) {
      startThread();
      Events.run(EventType.events.ThreadedStop, () -> stopThread());
    }

    return (T) this;
  }

  public T init(boolean runInThread, boolean pause) {
    
    if (pause) {
      Events.on(EventType.GamePause.class, event -> setPaused(event.pause));
    }
    
    return (T) init(runInThread);
  }

  public void update() {}

  public void dispose() {
    stopThread();
  }

  /**
   * 设置系统的暂停状态
   *
   * @param paused true为暂停，false为恢复
   */
  public void setPaused(boolean paused) {
    synchronized (pauseLock) {
      this.paused = paused;
      if (!paused) {
        // 如果是取消暂停，唤醒线程
        pauseLock.notifyAll();
      }
    }
  }

  /** 获取当前是否暂停 */
  public boolean isPaused() {
    return paused;
  }

  private void startThread() {
    if (threadRunning) return;

    threadRunning = true;

    systemThread =
        Threads.daemon(
            "System-" + this.getClass().getSimpleName(),
            () -> {
              Log.info("System thread started: @", this.getClass().getSimpleName());

              // 初始化基准时间
              lastTime = System.nanoTime();

              while (threadRunning) {
                try {
                  synchronized (pauseLock) {
                    // 如果处于暂停状态，且线程还运行，就挂起
                    while (paused && threadRunning) {
                      pauseLock.wait(); // 线程在此处停止，不消耗CPU

                      lastTime = System.nanoTime();
                    }
                  }

                  now = System.nanoTime();
                  elapsedNs = now - lastTime;
                  lastTime = now;

                  delta = (float) (elapsedNs / NS_PER_TICK);

                  delta = Math.min(delta, MAX_DELTA);
                  if (delta < 0.001f) delta = 0.001f;

                  logicStartNs = System.nanoTime();

                  update();

                  logicDurationMs = (System.nanoTime() - logicStartNs) / 1_000_000;
                  sleepTimeMs = threadSleepMs - logicDurationMs;

                  if (sleepTimeMs > 0) {
                    Thread.sleep(sleepTimeMs);
                  } else {
                    Thread.yield();
                  }

                } catch (InterruptedException e) {
                  threadRunning = false;
                  Thread.currentThread().interrupt(); // 恢复状态
                } catch (Exception e) {
                  Log.err("Error in system thread: @", this.getClass().getSimpleName(), e);
                }
              }
              Log.info("System thread stopped: @", this.getClass().getSimpleName());
            });
  }

  private void stopThread() {
    threadRunning = false;
    // 如果线程正在 wait() 状态，interrupt 会让它抛出 InterruptedException 从而退出循环
    if (systemThread != null) {
      systemThread.interrupt();
      try {
        systemThread.join(100);
      } catch (Exception ignored) {
      }
      systemThread = null;
    }
  }

  @Override
  public int compareTo(BasicSystem<?> other) {
    return this.index > other.index ? 1 : this.index < other.index ? -1 : 0;
  }
}
