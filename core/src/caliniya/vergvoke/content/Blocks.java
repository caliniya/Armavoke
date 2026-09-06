package caliniya.vergvoke.content;

import caliniya.vergvoke.type.type.BulletType;
import caliniya.vergvoke.world.*;
import caliniya.vergvoke.world.blocks.defence.Turret;

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
