package caliniya.armavoke.core.meta.stat;

import arc.Core;

/** 统计信息的组，例如战斗 */
public enum StatType {
  none("none"), // 无分组（类型名称/描述等顶部信息）
  fight("fight"), // 战斗
  power("power"), // 电力
  liquids("liquids"), // 液体
  items("items"), // 物品
  crafting("crafting"), // 生产
  general("general"), // 基础，通用
  function("function"), // 支持
  protect("protect"); // 防护
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
