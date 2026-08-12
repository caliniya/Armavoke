package caliniya.armavoke.core.meta.stat;

/** 统计值单元。绑定一个 {@link Stat} + 数值 + {@link StatUnit}。 */
public class StatData {
  public Stat stat;
  public float value;
  public StatUnit unit;

  public String data;

  public StatData(Stat stat, float value, StatUnit unit) {
    this.stat = stat;
    this.value = value;
    this.unit = unit;
    this.data = stat.localizedName + ": " + unit.format(value);
  }

  /** 原始文本条目（抗性列表等多行数据用），不绑定具体数值。 */
  public StatData(String raw) {
    this.data = raw;
  }
}
