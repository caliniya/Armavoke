package caliniya.armavoke.content;

import arc.util.Log;
import caliniya.armavoke.base.type.DamageType;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.ability.ShieldAbility;
import caliniya.armavoke.type.ability.ShieldFieldAbility;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.Armavoke;
import caliniya.armavoke.game.data.WorldData;

public class UnitTypes {

  public static UnitType test, test2;

  public static void load() {
    test =
        new UnitType("testunit") {
          {
            //this.armor = 500;
            this.armorMax = 500;
            this.armorValue = 30;
            //this.energy = 100;
            this.energyMax = 100;
            this.energyRegen = 10;
            // 类型级演示数据：护甲对动能 50% 抗性（DataWindow 可展示）
            this.armorResist[DamageType.Kinetic.ordinal()] = 0.5f;
            this.hitbox = new float[] {0f, 60f, 60f, 0f, 0f, 60f, 60f, 0f, 60f};

            addWeapons(
                new WeaponType("aa") {
                  {
                    mirror = true;
                    x = 50;
                    bullet = new BulletType();
                    rotate = true;
                  }
                });
            this.abilities.add(
                new ShieldFieldAbility(500, 200) {
                  {
                    this.regen = 10;
                    this.energyCost = 3;
                    this.resist[DamageType.Energy.ordinal()] = 0.6f;
                  }
                });
                this.abilities.add(
                  new ShieldAbility(200f){{
                    this.regen = 10;
                    this.energyCost = 3;
                    this.resist[DamageType.Thermal.ordinal()] = 0.6f;
                  }}
                );
          }
        };

    test2 =
        new UnitType("starNode") {
          {
            this.size = 128f;
          }
        };
  }
}
