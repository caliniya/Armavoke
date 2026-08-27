package caliniya.vergvoke.content;

import caliniya.vergvoke.world.Floor;

public class Floors {

  public static Floor TestFloor, space;

  public static void load() {
    TestFloor = new Floor("test"){{
    }};
    //space = new Floor("space");
  }
}
