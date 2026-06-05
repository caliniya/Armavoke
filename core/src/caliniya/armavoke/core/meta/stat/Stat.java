package caliniya.armavoke.core.meta.stat;

import arc.Core;
import caliniya.armavoke.base.tool.Ar;

public class Stat implements Comparable<Stat> {
  public static final Ar<Stat> all = new Ar<>();

  public final String name , localizedName;
  public final StatType type;
  public final int id;
  
  //表示某一种统计信息
  public Stat(String name , StatType type) {
    this.name = name;
    this.type = type;
    this.localizedName = Core.bundle.get("stat." + name);
    id = all.size;
    all.add(this);
  }

  @Override
  public int compareTo(Stat s) {
    return id - s.id;
  }
}
