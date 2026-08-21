package caliniya.armavoke.core.meta.stat;

import arc.func.Cons;
import caliniya.armavoke.base.tool.Ar;

/** 统计值单元 */
public class StatData {

  // 自身的信息类型
  public Stat stat;
  // 所属统计组，默认情况下 我们用通用
  public StatType type = StatType.general;
  // 对于布尔类型 除了"1"以外的值都视为否
  public float value;
  // 可选的最大值，默认情况下为-1
  public float valueMax = -1f;
  // 用于指定的缩进指数
  public int level = 1;
  // 单位
  public StatUnit unit;
  // 处理完毕后的值，应该是包含缩进的
  public String data;
  // 原始文本（不含缩进），add 重新生成缩进时使用
  public String raw = "";

  // 分组的另一种解决方法
  // 自身作为分组标题，该分组所属的内容 直接加入到自身
  public Ar<StatData> datas = new Ar<>();

  public StatData(Stat stat, float value) {
    this(stat, value, stat.unit);
  }

  public StatData(Stat stat, float value, float valueMax) {
    this(stat, value, stat.unit, 1, valueMax);
  }

  public StatData(Stat stat, float value, StatUnit unit) {
    this(stat, value, unit, 1, 0);
  }

  public StatData(Stat stat, float value, StatUnit unit, int level, float valueMax) {
    this.stat = stat;
    this.value = value;
    this.unit = unit;
    this.level = level;
    this.valueMax = valueMax;
    this.type = stat != null ? stat.type : StatType.none;
    this.raw = (stat != null ? stat.localizedName + ": " + unit.format(value) : "");
    this.data = indent() + raw;
  }

  /** 纯文本(比如说是介绍) */
  public StatData(String data) {
    this(data, 1, StatType.none);
  }

  public StatData(String data, int level) {
    this(data, level, StatType.none);
  }

  /** 纯文本 + 组/层级信息（标题等）。 */
  public StatData(String data, int level, StatType type) {
    this.level = level;
    this.type = type;
    this.raw = data;
    this.data = indent() + data;
  }

  public static StatData with(Stat stat, float value) {
    return new StatData(stat, value);
  }

  public static StatData with(Stat stat, float value, float valueMax) {
    return new StatData(stat, value, valueMax);
  }

  public static StatData with(Stat stat, float value, StatUnit unit) {
    return new StatData(stat, value, unit);
  }

  public static StatData with(Stat stat, float value, StatUnit unit, int level, float valueMax) {
    return new StatData(stat, value, unit, level, valueMax);
  }

  public static StatData with(String data) {
    return new StatData(data);
  }

  public static StatData with(String data, int level) {
    return new StatData(data, level);
  }

  public static StatData with(String data, int level, StatType type) {
    return new StatData(data, level, type);
  }

  /** 插入子元素：自动 level+1 并重新生成含缩进的 data。 */
  public StatData add(StatData child) {
    child.level = this.level + 1;
    child.data = child.indent() + child.raw;
    datas.add(child);
    return this;
  }

  public String indent() {
    return level <= 0 ? "   " : "\u3000\u3000".repeat(level);
  }

  // 包含自身以及子元素的递归
  public void each(Cons<StatData> con) {
    con.get(this);
    datas.each(d -> con.get(d));
  }
}
