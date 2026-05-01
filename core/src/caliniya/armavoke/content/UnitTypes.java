package caliniya.armavoke.content;

import arc.util.Log;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.Armavoke;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.game.type.UnitType;

public class UnitTypes {

  public static UnitType test, test2;

  public static void load() {
    test =
        new UnitType("testunit") {
          {
            // --- 新增：体积数据测试 ---
            // 设置基础尺寸(用于剔除判定参考)
            //this.size = 40f; 
            
            // 定义碰撞体积：一个横向的长条形 (2x1 格)
            // 假设单格大小为 20f。
            // 我们用两个 20x20 的正方形拼接而成。
            // 中心点1: (-10, 0), 中心点2: (10, 0)
            // 格式: [x, y, size, x, y, size...]
            this.hitbox = new float[] {
                //-10f, 0f, 20f, // 左边方块
                 //10f, 0f, 20f  // 右边方块
                 0f, 60f, 60f, // 竖直部分
                 0f,  0f, 60f,
                60f,  0f, 60f  // 横向突出部分
            };

            addWeapons(
                new WeaponType("aa") {
                  {
                    mirror = true;
                    x = 100;
                    bullet = new BulletType();
                    rotate = false;
                  }
                },
                new WeaponType("aa") {
                  {
                    mirror = true;
                    x = 50;
                    bullet = new BulletType();
                    rotate = true;
                  }
                });
            this.load();
          }
        };
    /*
    test2 =
        new UnitType("testunit") {
          {
            // 你也可以在这里测试 L 形或 T 形
            this.size = 60f;
            // L 形示例
            this.hitbox = new float[] {
                 0f, 20f, 20f, // 竖直部分
                 0f,  0f, 20f,
                20f,  0f, 20f  // 横向突出部分
            };
            
            addWeapons( ... );
            this.load();
          }
        };
        */
  }
}