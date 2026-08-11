package caliniya.armavoke.ui.windows;

import arc.*;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.ability.ShieldAbility;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.ui.fragment.HUDFragment;
import caliniya.armavoke.ui.fragment.UniverseFragment;

public class PauseWindow extends Window {
  /** 呃啊 */
  public PauseWindow() {
    super("@pauseWindow");
    w = Core.graphics.getWidth() / 2f;
    h = Core.graphics.getHeight() / 2f;
    modal = true;
  }

  @Override
  public void main(Table t) {
    // 单例反复 build()，先清空避免按钮累积
    t.clearChildren();
    t.add(
            new Button(
                "宇宙",
                () -> {

                  // 同步宇宙相机到游戏相机位置
                  Render.universeCamera.position.set(Core.camera.position);
                  Render.universeCamera.width = Core.camera.width;
                  Render.universeCamera.height = Core.camera.height;
                  UI.universe.build();
                  UI.hud.hideHUD();
                  this.window.visible = false;
                  this.modalOverlay.visible = false;
                }))
        .size(120f, 50f)
        .left()
        .top();
    t.row();
    t.add(new Button("科技树", () -> new TechTreeWindow().build())).size(120f, 50f).left().top();
    t.row();
    t.add(new Button("战斗测试", () -> testCombat())).size(120f, 50f).left().top();
  }

  /** 战斗系统测试（验证 Step1/2 的伤害结算），结果打印到日志。 预期值基于：护甲强度 30，满盾 500（最大强度 2），护盾按容量比例减伤。 */
  private void testCombat() {
    Log.info("========== 战斗系统测试开始 ==========");

    if (WorldData.world == null) {
      Log.warn("[测试] 世界未初始化，请先进入一局游戏");
      return;
    }

    Unit u = UnitTypes.test.create(TeamTypes.Evoke, 0, 0);
    u.health = 500;
    u.maxHealth = 500;
    u.armor = 30;

    ShieldAbility shield = new ShieldAbility(500);
    shield.regen = 0;
    shield.energyCost = 0;
    u.add(shield);

    Log.info("[1] 初始: 盾=@ 血=@ (预期 盾500/血500)", shield.current, u.health);

    // 满盾(比例1.0, 强度2)被能量100打：100×1.5/2 = 75
    u.applyDamage(100f, DamageType.Energy);
    Log.info("[2] 满盾能量100: 盾=@ 血=@ (预期 盾425/血500)", shield.current, u.health);

    // 盾425(比例0.85, 强度1.7)能量100：100×1.5/1.7 ≈ 88.2
    u.applyDamage(100f, DamageType.Energy);
    Log.info("[3] 盾425能量100: 盾=@ 血=@ (预期 盾≈336.8/血500)", shield.current, u.health);

    // 动能1000打穿护盾，溢出不传递 → 盾0，血不变
    u.applyDamage(1000f, DamageType.Kinetic);
    Log.info("[4] 破盾动能1000: 盾=@ 血=@ (预期 盾0/血500, 溢出不传递)", shield.current, u.health);

    // 无盾动能100：100×1.0-30 = 70
    u.applyDamage(100f, DamageType.Kinetic);
    Log.info("[5] 无盾动能100: 血=@ (预期 430)", u.health);

    // 无盾热能100：100×1.5-30 = 120
    u.applyDamage(100f, DamageType.Thermal);
    Log.info("[6] 无盾热能100: 血=@ (预期 310)", u.health);

    // 低于护甲的伤害完全挡下
    u.applyDamage(10f, DamageType.Kinetic);
    Log.info("[7] 无盾动能10(<护甲30): 血=@ (预期 310, 完全减伤)", u.health);

    // 护甲对动能 50% 抗性：100×1.0×0.5-30 = 20
    u.armorResist[DamageType.Kinetic.ordinal()] = 0.5f;
    u.applyDamage(100f, DamageType.Kinetic);
    Log.info("[8] 护甲动能抗性50%: 血=@ (预期 290)", u.health);

    u.remove();
    Log.info("========== 战斗系统测试结束 ==========");
  }
}
