package caliniya.armavoke.core.meta.stat;

import arc.Core;

/** 统计信息的组，例如战斗 */
public enum StatType {
    fight("fight"),
    power("power"),
    liquids("liquids"),
    items("items"),
    crafting("crafting"),
    general("general"),
    function("function");

    public final String name, localizedName;

    StatType(String name) {
        this.name = name;
        this.localizedName = Core.bundle.get("statType." + name);
    }

    @Override
    public String toString() {
        return name;
    }
}
