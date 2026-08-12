package caliniya.armavoke.type;

import arc.struct.ObjectMap;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.type.enhance.EnhancementType;

/**
 * 插件实体（实例，仅一个类）：单位运行时安装的"战术增强"，只存数据，<b>行为由类型定义</b>。
 *
 * <p>类似 {@code Unit → UnitType}：实体把自己传给类型操作——开关/绑定/开启/关闭的具体行为 在 {@link
 * EnhancementType#onEnable}/{@link EnhancementType#onDisable}/{@link EnhancementType#rebind} 中实现。
 */
public class Enhancement {

  /** 所属插件类型（配置 + 行为来源）。 */
  public EnhancementType type;

  /** 插件开关（默认开启）。 */
  public boolean enabled = true;

  /** 绑定目标：实体（挂载时自动设置）。 */
  public Entity entity;

  /** 绑定目标：能力（可选，由类型 {@code rebind} 填充）。 */
  public Ability ability;

  /** 类型专属运行时数据（备份等），由类型覆写方法存取。 */
  public final ObjectMap<String, Object> vars = new ObjectMap<>();

  /** 开关切换：转交类型执行 onEnable/onDisable。 */
  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) return;
    this.enabled = enabled;
    if (enabled) {
      type.onEnable(this);
    } else {
      type.onDisable(this);
    }
  }

  /** 序列化实例状态：先写通用开关，再委托类型写专属运行数据。 */
  public void write(Writes w) {
    w.bool(enabled);
    if (type != null) {
      type.write(this, w);
    }
  }

  /** 反序列化实例状态（与 {@link #write} 对应）。 */
  public void read(Reads r) {
    enabled = r.bool();
    if (type != null) {
      type.read(this, r);
    }
  }
}
