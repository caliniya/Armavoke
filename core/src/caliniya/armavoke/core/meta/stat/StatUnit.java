package caliniya.armavoke.core.meta.stat;

import arc.Core;
import arc.util.Nullable;
import java.util.Locale;

/** 数值单位定义。控制统计信息中数字的显示格式。 */
public class StatUnit {
  public static final StatUnit 
      none = new StatUnit("none"), // 无单位
      percent = new StatUnit("percent", false), // 百分比
      multiplier = new StatUnit("multiplier", false), // 倍率
      perSecond = new StatUnit("perSecond", false), // 每秒
      perMinute = new StatUnit("perMinute", false), // 每分钟
      perShot = new StatUnit("perShot", false), // 每次射击
      timesSpeed = new StatUnit("timesSpeed", false), // 倍速
      blocks = new StatUnit("blocks"), // 格（方块数）
      blocksSquared = new StatUnit("blocksSquared"), // 平方格
      tilesSecond = new StatUnit("tilesSecond"), // 格/秒
      degrees = new StatUnit("degrees"), // 度数
      seconds = new StatUnit("seconds"), // 秒
      minutes = new StatUnit("minutes"), // 分钟
      shots = new StatUnit("shots"), // 发数（射击次数）
      items = new StatUnit("items"), // 物品数量
      itemsSecond = new StatUnit("itemsSecond") // 物品/秒
  ;

  public final String name;
  public final boolean space;
  public @Nullable String icon;
  public final String localizedName;

  public StatUnit(String name, boolean space) {
    this.name = name;
    this.space = space;
    this.localizedName = Core.bundle.get("statUnit." + name.toLowerCase(Locale.ROOT));
  }

  public StatUnit(String name) {
    this(name, true);
  }

  public StatUnit(String name, String icon) {
    this(name, true);
    this.icon = icon;
  }
}
