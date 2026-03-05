package caliniya.armavoke.content;

import caliniya.armavoke.type.type.ItemType;

public class Items {

  public static ItemType Ge;

  public static void load() {
    Ge =
        new ItemType("germanium") {
          {
          }
        };
  }
}
