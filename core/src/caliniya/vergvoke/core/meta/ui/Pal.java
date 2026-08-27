package caliniya.vergvoke.core.meta.ui;

import arc.graphics.*;

public class Pal {
  
  public static Color light = Color.valueOf("03ECED");

  public static void load() {
    Colors.put("light", light);
  }

  public static String format(Color color, String text) {
    return "[#" + color + "]" + text + "[]";
  }
}
