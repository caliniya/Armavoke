package caliniya.armavoke.core.meta.stat;

import caliniya.armavoke.base.tool.Ar;

/** 统计值单元 */
public class StatData {

  // 自身所属的信息类型
  public Stat stat;
  // 对于布尔类型 除了一以外的值都视为否
  public float value;
  // 可选的最大值，默认情况下为-1
  public float valueMax = -1f;
  // 所属的组名称，默认为null，不同组的名称可以相同
  public String group = null;
  // 对于同名的组 这个用于区分
  public int groupKey = 0;
  // 用于指定型的缩进指数
  public int level = 0;
  // 单位
  public StatUnit unit;
  // 处理完毕后的值，应该是包含缩进的
  public String data;

  // 分组的另一种解决方法
  public Ar<StatData> datas = new Ar<>();

  public StatData(Stat stat, float value, StatUnit unit) {
    this.stat = stat;
    this.value = value;
    this.unit = unit;
    this.data = indent() + stat.localizedName + ": " + unit.format(value);
  }

  public StatData(
      Stat stat,
      float value,
      StatUnit unit,
      int level,
      int groupKey,
      String group,
      float valueMax) {
    this.stat = stat;
    this.value = value;
    this.unit = unit;
    this.level = level;
    this.groupKey = groupKey;
    this.group = group;
    this.valueMax = valueMax;
  }

  /** 纯文本(比如说是介绍) */
  public StatData(String data) {
    this(data, 0);
  }

  public StatData(String data, int level) {
    this.level = level;
    this.data = indent() + data;
  }

  public String indent() {
    return level <= 0 ? "   " : "\u3000\u3000".repeat(level);
  }
}
