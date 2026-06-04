package caliniya.armavoke.core.meta.stat;

/**
 * 单条统计数据 — name + value + unit 的轻量三元组。
 */
public class StatData {
    public final String name;
    public final float value;
    public final StatUnit unit;

    public StatData(String name, float value, StatUnit unit) {
        this.name = name;
        this.value = value;
        this.unit = unit;
    }
}
