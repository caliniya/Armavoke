package caliniya.armavoke.core.meta.stat;

import arc.Core;
import arc.util.Nullable;
import arc.util.Strings;
import java.util.Locale;

/** 数值单位定义。控制统计信息中数字的显示格式。 */
public enum StatUnit {
    none("none"),
    percent("percent", false),
    multiplier("multiplier", false),
    perSecond("perSecond", false),
    perMinute("perMinute", false),
    perShot("perShot", false),
    timesSpeed("timesSpeed", false),
    blocks("blocks"),
    blocksSquared("blocksSquared"),
    tilesSecond("tilesSecond"),
    degrees("degrees"),
    seconds("seconds"),
    minutes("minutes"),
    shots("shots"),
    items("items"),
    itemsSecond("itemsSecond");

    public final String name;
    public final boolean space;
    public @Nullable String icon;
    public final String localizedName;

    StatUnit(String name, boolean space) {
        this.name = name;
        this.space = space;
        this.localizedName = Core.bundle.get("statUnit." + name.toLowerCase(Locale.ROOT));
    }

    StatUnit(String name) {
        this(name, true);
    }

    public StatUnit icon(String icon) {
        this.icon = icon;
        return this;
    }

    /** 格式化数值 */
    public String format(float value) {
        if (this == none) return Strings.autoFixed(value, 2);
        if (this == percent) return Strings.autoFixed(value * 100, 2) + localizedName;
        if (this == multiplier) return "×" + Strings.autoFixed(value, 2) + localizedName;
        return Strings.autoFixed(value, 2) + (space ? " " : "") + localizedName;
    }
}
