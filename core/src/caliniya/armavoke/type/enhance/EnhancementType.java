package caliniya.armavoke.type.enhance;

import caliniya.armavoke.base.api.TechNodeContent;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.type.Enhancement;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.core.meta.stat.StatType;
import arc.util.io.Reads;
import arc.util.io.Writes;

/**
 * 插件类型（模板）：定义一种插件的配置与来源，注册进内容表并可挂入科技树。
 *
 * <p>给单位安装插件 = 依据类型 {@link #create()} 创建插件实体，再挂载到单位。
 */
public abstract class EnhancementType extends ContentType implements TechNodeContent {

  public EnhancementType(String name) {
    this(name, true);
  }

  /**
   * @param register 是否注册进内容表（false = 运行时临时类型，如测试用不同配置）
   */
  public EnhancementType(String name, boolean register) {
    super(name, CType.Enhance, register);
  }

  /** 依据类型创建插件实体（配置拷贝 + 实例初始化）。 */
  public Enhancement create() {
    Enhancement e = new Enhancement();
    e.type = this;
    return e;
  }

  /** 挂载/读档后恢复绑定（从实体重新查找目标能力，操作实体 {@code e}）。 */
  public void rebind(Enhancement e) {}

  /** 开启时应用强化（配置用本类型字段，运行时备份存 {@code e.vars}）。 */
  public void onEnable(Enhancement e) {}

  /** 关闭时恢复原值。 */
  public void onDisable(Enhancement e) {}

  /** 上报类型数据（名称/描述，无分组，供详情窗口展示）。 */
  public void stats(StatStack stack) {
    stack.groupStart(localizedName);
    if (description != null) {
      stack.addRaw(description);
    }
    stack.groupEnd();
  }

  /**
   * 序列化实体运行数据（复杂插件的独立状态，存于 {@code e.vars} 或实体字段）。
   * 基类 {@link Enhancement#write} 先写 enabled，再委托本方法写类型专属数据。
   */
  public void write(Enhancement e, Writes w) {}

  /** 反序列化实体运行数据（与 {@link #write} 对应）。 */
  public void read(Enhancement e, Reads r) {}

  @Override
  public TechNodeContent[] requirements() {
    return requirements;
  }
}
