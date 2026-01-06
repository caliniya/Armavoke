package caliniya.armavoke.content;

import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.Armavoke;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.game.type.UnitType;

public class UnitTypes {

  public static UnitType test;

  public static Unit ttt;

  public static void load() {
    test =
        new UnitType("testunit") {
          {
            this.addWeapons(
                new WeaponType("aa") {
                  {
                    mirror = true;
                    x = 100;
                    bullet = new BulletType();
                    rotate = false;
                  }
                });
            this.load();
          }
        };
  }
}
