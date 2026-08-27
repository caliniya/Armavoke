package caliniya.vergvoke.content;

import caliniya.vergvoke.type.type.ItemType;

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
