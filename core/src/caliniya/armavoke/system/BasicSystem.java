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
  private final Object pauseLock = new Object();

  private Thread systemThread;

  // 目标帧率 60FPS -> 每帧 16,666,666 ns
  protected long targetNs = 16_666_666L;

  public float delta = 1f;
  private static final double NS_PER_TICK = 1_000_000_000.0 / 60.0;
  private static final float MAX_DELTA = 4.0f;

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

  public void setPaused(boolean paused) {
    synchronized (pauseLock) {
      this.paused = paused;
      if (!paused) {
        pauseLock.notifyAll();
      }
    }
  }

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

              long lastTime = System.nanoTime();
              long timer = System.currentTimeMillis(); // 用于防止螺旋死亡(Spiral of Death)的可选检查

              while (threadRunning) {
                try {
                  // 1. 处理暂停
                  synchronized (pauseLock) {
                    while (paused && threadRunning) {
                      pauseLock.wait();
                      lastTime = System.nanoTime(); // 唤醒后重置时间，防止巨大的 delta
                    }
                  }

                  long now = System.nanoTime();
                  long elapsedNs = now - lastTime;
                  lastTime = now;

                  // 2. 计算 Delta
                  // 使用双精度浮点数计算，减少累积误差
                  delta = (float) (elapsedNs / NS_PER_TICK);

                  // 钳制 delta，防止极端卡顿后物理瞬移
                  if (delta > MAX_DELTA) delta = MAX_DELTA;
                  // 防止除以零或其他算术错误
                  if (delta < 0.0001f) delta = 0.0001f;

                  // 3. 执行逻辑
                  long updateStart = System.nanoTime();
                  update();
                  long updateEnd = System.nanoTime();

                  // 4. 精确休眠控制
                  long logicDuration = updateEnd - updateStart;
                  long sleepNs = targetNs - logicDuration;

                  if (sleepNs > 0) {
                    long sleepMs = sleepNs / 1_000_000;
                    int sleepNanoPart = (int) (sleepNs % 1_000_000);

                    // 策略：如果剩余时间 > 1ms，使用 Thread.sleep
                    // 否则使用忙等待 (Busy Wait) 以保证精度
                    if (sleepMs > 1) {
                      // 这里故意少睡 1ms，留给下面的忙等待来修正精度
                      Thread.sleep(sleepMs - 1);
                    }

                    // 忙等待直到达到目标时间
                    while (System.nanoTime() - updateEnd < sleepNs) {
                      // 自旋，不让出 CPU，获得纳秒级精度
                      // Thread.onSpinWait(); // Java 9+ 可用，Android 可能没有
                    }
                  } else {
                    // 如果逻辑超时了，让出一点时间片防止死锁
                    Thread.yield();
                  }

                } catch (InterruptedException e) {
                  threadRunning = false;
                  Thread.currentThread().interrupt();
                } catch (Exception e) {
                  Log.err("Error in system thread: @", this.getClass().getSimpleName(), e);
                }
              }
              Log.info("System thread stopped: @", this.getClass().getSimpleName());
            });
  }

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

  @Override
  public int compareTo(BasicSystem<?> other) {
    return Integer.compare(this.index, other.index);
  }
}
