package caliniya.armavoke.core.meta.stat;

import arc.Core;
import caliniya.armavoke.base.tool.Ar;

public enum Stat {
  health("health", StatType.general);
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
