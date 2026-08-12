package caliniya.armavoke.content;

import arc.util.Log;
import caliniya.armavoke.type.*;
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
                    this.regen = 50;
                    this.cost = 3;
                  }
                });
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
