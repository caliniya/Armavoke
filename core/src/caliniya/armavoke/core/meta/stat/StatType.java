package caliniya.armavoke.core.meta.stat;

import arc.*;
import caliniya.armavoke.base.tool.Ar;

/** 统计信息的组，例如战斗 */
public class StatType implements Comparable<StatType> {
  public static final Ar<StatType> all = new Ar<>();

  public static final StatType 
      fight = new StatType("fight"), // 战斗
      power = new StatType("power"), // 电力
      liquids = new StatType("liquids"), // 液体
      items = new StatType("items"), // 物品
      crafting = new StatType("crafting"), // 生产
      general = new StatType("general"), //通用
      function = new StatType("function"); // 功能/支持

  public final String name , localizedName;
  public final int id;

  public StatType(String name) {
    this.name = name;
    localizedName = Core.bundle.get("statType." + name);
    id = all.size;
    all.add(this);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(StatType o) {
    return id - o.id;
  }
}
