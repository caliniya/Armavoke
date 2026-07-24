package caliniya.armavoke.content;

import caliniya.armavoke.type.type.*;
import caliniya.armavoke.world.*;
import caliniya.armavoke.world.defence.turret.*;

public class Blocks {

  public static Block TestBlock;
  public static Turret testTurret;

  public static void load() {
    TestBlock =
        new Block("test-building") {
          {
            this.size = 3;
          }
        };
    testTurret =
        new Turret("testturret") {
          {
            this.size = 3;
            this.bulletType =
                new BulletType() {
                  {
                  }
                };
          }
        };
  }
}
