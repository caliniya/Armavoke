package caliniya.armavoke.core.meta.stat;

import arc.Core;
import caliniya.armavoke.base.tool.Ar;

public enum Stat {
  Name("name", StatType.none),
  info("info", StatType.none),
  // 基础
  health("health", StatType.general),
  speed("speed", StatType.general),
  rotateSpeed("rotateSpeed", StatType.general),
  energyMax("energyMax", StatType.general),
  energyRegen("energyRegen", StatType.general),
  // 防护
  armor("armor", StatType.protect),
  armorValue("armorValue", StatType.protect),
  // 支持
  shield("shield", StatType.function),
  shieldStrength("shieldStrength", StatType.function),
  shieldRegen("shieldRegen", StatType.function),
  heat("heat", StatType.function),
  shieldCost("shieldCost", StatType.function),
  radius("radius", StatType.function);
  public final String name, localizedName;
  public final StatType type;

  // 表示某一种统计信息，例如生命
  Stat(String name, StatType type) {
    this.name = name;
    this.type = type;
    this.localizedName = Core.bundle.get("stat." + name);
  }

  @Override
  public String toString() {
    return "Stat." + this.name;
  }
}
