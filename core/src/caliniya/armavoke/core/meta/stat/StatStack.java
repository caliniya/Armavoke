package caliniya.armavoke.core.meta.stat;

import caliniya.armavoke.base.tool.Ar;

/**
 * 统计信息堆栈，收集并暴露一组 {@link StatData}。
 */
public class StatStack {
    private final Ar<StatData> stats = new Ar<>();

    /** 添加一条 stat，value 原样保留。 */
    public void add(String name, float value, StatUnit unit) {
        stats.add(new StatData(name, value, unit));
    }

    /** 添加一条 stat，整数按 float 存入。 */
    public void add(String name, int value, StatUnit unit) {
        stats.add(new StatData(name, value, unit));
    }

    public Ar<StatData> all() {
        return stats;
    }

    public int size() {
        return stats.size;
    }
}
