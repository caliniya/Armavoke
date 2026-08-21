package caliniya.armavoke.base.game;

import arc.Core;
import arc.util.Nullable;
import caliniya.armavoke.base.api.TechNodeContent;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.core.meta.stat.StatType;
import caliniya.armavoke.game.*;

public class ContentType {

  public final String name;
  public final CType type;

  // 命名空间名称
  public final String internalName;

  public StatStack stat;

  // ID 从 1 开始，0 保留为空（int，内存占用无所谓）
  public int id;

  // 本地化名称
  public String localizedName;
  public @Nullable String description;

  // 科技树相关：不参与科技的内容（如环境方块）保持 null 即可
  /** 科技树前置内容（null = 不参与科技树 / 无前置）。 */
  public TechNodeContent[] requirements = null;

  public ContentType(String name, CType type) {
    this(name, type, true);
  }

  /**
   * @param register 是否注册进内容表。战役等设计好的内容为 true（分配 ID）； 运行时临时/程序生成的内容可传 false，避免占用内容 ID。
   */
  protected ContentType(String name, CType type, boolean register) {
    this.name = name;
    this.type = type;

    this.internalName = type.name() + "." + name;

    this.localizedName = Core.bundle.get(internalName + ".name", name);
    this.description = Core.bundle.getOrNull(internalName + ".description");

    stat = new StatStack();

    stat.add(localizedName, StatType.none);
    if (description != null) {
      stat.add(description, StatType.none);
    }

    if (register) {
      // 注册时会自动分配 ID
      Contents.add(this);
    }
  }

  public void load() {}

  public String getIdentity() {
    return internalName;
  }

  @Override
  public String toString() {
    return internalName;
  }
}
