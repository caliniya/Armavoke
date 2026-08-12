package caliniya.armavoke.type.enhance;

import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.type.ability.Ability;

/**
 * 强化模组基类：单位的"战术增强"（插件/模组）。
 *
 * <p>与能力（Ability）分工：能力是常驻机制（护盾/热量），强化是可快速开关的增强—— <b>开启时修改目标数值，关闭时恢复原值</b>，灵活应变。
 *
 * <p>三种用途通过实现接口区分（可组合）：
 *
 * <pre>
 * {@link EntityBind}  —— 绑定实体（强化实体属性）
 * {@link AbilityBind} —— 绑定能力（强化实体已有能力）
 * {@link Updatable}   —— 每帧逻辑（感应/推进等临时机制）
 * </pre>
 */
public abstract class Enhancement implements Cloneable {

  /** 强化开关（默认开启）。 */
  public boolean enabled = true;

  /** 绑定目标：实体（挂载时自动设置）。 */
  public Entity entity;

  /** 绑定目标：能力（可选，绑定已有能力时由 {@link AbilityBind} 实现者填充）。 */
  public Ability ability;

  /** 开关切换：开启 → onEnable（应用修改），关闭 → onDisable（恢复原值）。 */
  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) return;
    this.enabled = enabled;
    if (enabled) {
      onEnable();
    } else {
      onDisable();
    }
  }

  /** 开启时应用强化。 */
  public void onEnable() {}

  /** 关闭时恢复原值。 */
  public void onDisable() {}

  /** 深拷贝（清空绑定引用，挂载时重新绑定）。 */
  public Enhancement copy() {
    try {
      Enhancement e = (Enhancement) this.clone();
      e.entity = null;
      e.ability = null;
      return e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** 序列化运行时状态（开关）。配置数据由子类覆写补充。 */
  public void write(Writes w) {
    w.bool(enabled);
  }

  /** 反序列化运行时状态，覆盖配置/开关。 */
  public void read(Reads r) {
    enabled = r.bool();
  }

  /** 读档/挂载后恢复绑定（绑定型插件覆写：从实体重新查找目标能力）。 */
  public void rebind(Entity e) {}

  /** 按类名反射创建增强模组实例（读档用）。 */
  public static Enhancement create(String className) {
    try {
      Class<?> c = Class.forName(className);
      return (Enhancement) c.getConstructor().newInstance();
    } catch (Exception e) {
      Log.err("Cannot create enhancement: @", className, e);
      return null;
    }
  }
}
