package caliniya.armavoke.core;

import arc.*;
import arc.*;
import arc.files.*;
import arc.math.*;
import arc.math.*;
import arc.util.*;
import caliniya.armavoke.*;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.content.*;

import caliniya.armavoke.content.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.io.*;
import caliniya.armavoke.io.*;
import caliniya.armavoke.map.*;
import caliniya.armavoke.system.game.*;
import caliniya.armavoke.system.input.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.world.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.system.*;

import static caliniya.armavoke.game.data.WorldData.*;

public class Data {

  static {
    // Events.on(EventType.GameInit.class, evevt -> testinit());
  }

  // 从指定文件加载整个地图，使用异步
  // 这一步仅加载所有数据，理论上讲 不应该影响任何游戏界面
  public static void load(Fi file) {
    GameIO.load(file, d -> DataIO.load(d));
  }

  // 这个方法会加载所有的系统
  // 所有渲染方法 在此阶段不应该启动
  public static void loadSystems() {
    Systems.MR = new MapRender();
    Systems.UR = new UnitRender();
    Systems.BR = new BlockRender();
    Systems.GP = new GameProcess();
    Systems.UV = new UniverseRender();
    Systems.UI = new UniverseInput();
    //Systems.DE = new DebugRender();
    Systems.addSystem(
        Systems.MR, Systems.UR, Systems.GP, Systems.BR, Systems.UV, Systems.UI, new Render());
    Systems.BP = new BulletProcess();
    Systems.UM = new UnitMath();
    Systems.EP = new EntityProces();
  }

  // 初始化所有系统，允许其工作
  // 同时将游戏UI切换到游戏内部
  public static void enter() {
    for (caliniya.armavoke.system.System sys : Systems.systems) {
      sys.init();
    }
    Systems.BP.init();
    Systems.UM.init();
    Systems.EP.init();

    UI.Game();
  }
}
