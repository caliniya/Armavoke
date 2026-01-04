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

  // 目标帧率 60FPS -> 约 16.6 ms
  // 用于限制后台线程的 CPU 占用率
  protected long targetNs = 16_666_666L;

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

              while (threadRunning) {
                try {
                  // 1. 处理暂停
                  synchronized (pauseLock) {
                    while (paused && threadRunning) {
                      pauseLock.wait();
                    }
                  }

                  // 2. 记录开始时间
                  long startTime = System.nanoTime();

                  // 3. 执行逻辑 (包含内部异常捕获，防止单次错误杀掉线程)
                  try {
                    update();
                  } catch (Exception e) {
                    Log.err("Error in system thread: @", this.getClass().getSimpleName(), e);
                  }

                  // 4. 计算休眠时间
                  long endTime = System.nanoTime();
                  long elapsedNs = endTime - startTime;
                  long sleepNs = targetNs - elapsedNs;

                  if (sleepNs > 0) {
                    long sleepMs = sleepNs / 1_000_000;
                    int sleepNanoPart = (int) (sleepNs % 1_000_000);

                    Thread.sleep(sleepMs, sleepNanoPart);
                  } else {
                    // 如果运算超时，让出时间片，防止饿死其他线程
                    Thread.yield();
                  }

                } catch (InterruptedException e) {
                  threadRunning = false;
                  Thread.currentThread().interrupt(); // 恢复中断状态
                } catch (Exception e) {
                  // 捕获循环体外部的异常（如 sleep 以外的错误）
                  Log.err("Critical error in system loop: @", this.getClass().getSimpleName(), e);
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
        // 等待线程稍微清理一下，避免立即销毁导致资源未释放
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
