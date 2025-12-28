package caliniya.armavoke.base.type;

public class EventType {
  
  public enum events{
    Mapinit,
    ThreadedStop,//线程终止
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
