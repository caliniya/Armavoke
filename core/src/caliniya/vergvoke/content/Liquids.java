package caliniya.vergvoke.content;

import arc.graphics.Color;
import caliniya.vergvoke.type.type.LiquidType;

/** 液体类型注册（示例）。 */
public class Liquids {

  /** 燃料（高能量密度）。 */
  public static LiquidType fuel;

  /** 冷却剂（高热容量）。 */
  public static LiquidType coolant;

  public static void load() {
    fuel =
        new LiquidType("fuel") {
          {
            color = Color.valueOf("FFB300");
            viscosity = 0.4f;
            energyDensity = 10f;
            heatCapacity = 0.2f;
          }
        };

    coolant =
        new LiquidType("coolant") {
          {
            color = Color.valueOf("4FC3F7");
            viscosity = 0.3f;
            energyDensity = 0f;
            heatCapacity = 5f;
          }
        };
  }
}
