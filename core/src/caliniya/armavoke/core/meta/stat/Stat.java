package caliniya.armavoke.core.meta.stat;

import arc.Core;
import caliniya.armavoke.base.tool.Ar;

public enum Stat {
  // 基础
  health("health", StatType.general),
  speed("speed", StatType.general),
  rotateSpeed("rotateSpeed", StatType.general),
  energyRegen("energyRegen", StatType.general),
  // 防护
  armor("armor", StatType.protect),
  armorValue("armorValue", StatType.protect),
  shield("shield", StatType.protect),
  shieldStrength("shieldStrength", StatType.protect),
  shieldRegen("shieldRegen", StatType.protect),
  // 支持（能力/热量）
  heat("heat", StatType.function);
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
