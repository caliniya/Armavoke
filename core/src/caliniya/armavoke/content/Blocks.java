package caliniya.armavoke.content;

import caliniya.armavoke.type.type.BulletType;
import caliniya.armavoke.world.*;
import caliniya.armavoke.world.defence.turret.Turret;

public class Blocks {

  public static Block TestBlock;
  public static Turret testTurret;

  public static void load() {
    TestBlock =
        new Block("test-building") {
          {
            this.size = 3;
            this.load();
          }
        };
    testTurret =
        new Turret("testturret") {
          {
            this.bulletType =
                new BulletType() {
                  {
                  }
                };
            this.load();
          }
        };
  }
}
