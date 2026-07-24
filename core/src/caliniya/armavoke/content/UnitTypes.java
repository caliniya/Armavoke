package caliniya.armavoke.content;

import arc.util.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.*;
import caliniya.armavoke.game.data.*;

public class UnitTypes {

  public static UnitType test, test2;

  public static void load() {
    test =
        new UnitType("testunit") {
          {
            this.hitbox =
                new float[] {
                  0f,
                  60f,
                  60f, 
                  0f,
                  0f,
                  60f,
                  60f,
                  0f,
                  60f 
                };

            addWeapons(
                new WeaponType("aa") {
                  {
                    mirror = true;
                    x = 50;
                    bullet = new BulletType();
                    rotate = true;
                  }
                });
          }
        };
    /*
    test2 =
        new UnitType("testunit") {
          {
            // 你也可以在这里测试 L 形或 T 形
            this.size = 60f;
            // L 形示例
            this.hitbox = new float[] {
                 0f, 20f, 20f, // 竖直部分
                 0f,  0f, 20f,
                20f,  0f, 20f  // 横向突出部分
            };

            addWeapons( ... );
            this.load();
          }
        };
        */
  }
}
