package caliniya.armavoke.content;

import caliniya.armavoke.type.type.BulletType;
import caliniya.armavoke.world.*;
import caliniya.armavoke.world.defence.turret.Turret;
import caliniya.armavoke.type.Item;
import caliniya.armavoke.world.blocks.produce.recipe.Recipe;
import caliniya.armavoke.world.blocks.produce.unit.Factory;

public class Blocks {

  public static Block TestBlock;
  public static Turret testTurret;
  public static Factory unitFactory;

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

    unitFactory =
        new Factory("unit-factory") {
          {
            size = 3;
            capacity = 200;
            powerCapacity = 100f;
            liquidCapacity = 100f;
            allowAllItem(Items.Ge);
            recipes(
                new Recipe(
                    UnitTypes.test.localizedName,
                    UnitTypes.test,
                    5f,
                    new Item(Items.Ge, 10)));
          }
        };
  }
}
