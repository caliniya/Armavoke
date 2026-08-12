package caliniya.armavoke.content;

import caliniya.armavoke.type.enhance.EnhancementType;
import caliniya.armavoke.type.enhance.shield.ShieldBoostEnhancementType;

/** 插件类型注册表：所有可安装的插件类型定义在这里（类似 UnitTypes / Blocks）。 */
public class Enhancements {

  /** 护盾强化插件（类型级默认配置）。 */
  public static EnhancementType shieldBoost;

  public static void load() {
    shieldBoost =
        new ShieldBoostEnhancementType() {
          {
            // 默认配置（可在此调整，或在游戏内用 create 后覆盖）
            this.maxStrengthBonus = 1f;
            this.kineticResistBonus = 0.3f;
          }
        };
  }
}
