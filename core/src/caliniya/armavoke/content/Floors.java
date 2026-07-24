package caliniya.armavoke.content;

import caliniya.armavoke.world.*;

public class Floors {

  public static Floor TestFloor, space;

  public static void load() {
    TestFloor = new Floor("test"){{
    }};
    //space = new Floor("space");
  }
}
