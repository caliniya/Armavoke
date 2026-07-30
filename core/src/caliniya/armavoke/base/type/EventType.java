package caliniya.armavoke.base.type;

public class EventType {

  public enum events {
    Mapinit,
    ThreadedStop, // 线程终止
    StartLoad, // 开始从存储中加载内容
    FinishLoad, // 完成加载，所有线程系统可以开始继续工作
    StartSave, // 开始保存，所有系统应立即暂停(？)
    EnterUV,// 切换到宇宙视图
    ExitUV// 退出
  ;
  }

  public static class GameInit {}

  public static class CommandChange {
    public final boolean enabled;

    public CommandChange(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class GamePause {
    public final boolean pause;

    public GamePause(boolean pause) {
      this.pause = pause;
    }
  }
}
