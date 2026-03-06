package caliniya.armavoke;

import arc.Core;
import static arc.Core.*;

import arc.*;
import arc.assets.Loadable;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.Scene;
import arc.scene.ui.layout.Scl;
import arc.util.Log;
import arc.util.viewport.ScreenViewport;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.content.*;
import caliniya.armavoke.core.ContentVar;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.system.*;
import caliniya.armavoke.system.input.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.world.*;
import caliniya.armavoke.ui.*;
import caliniya.armavoke.ui.fragment.*;
import caliniya.armavoke.type.type.*;

public class Armavoke extends ApplicationCore {

  public boolean assinited = false;
  public CameraInput camInput;

  // 用于记录开始时间
  private long startTime;

  public static Ar<caliniya.armavoke.system.System> systems =
      new Ar<caliniya.armavoke.system.System>(10);

  @Override
  public void setup() {
    // 记录应用启动时的纳秒时间
    startTime = java.lang.System.nanoTime();
    graphics.clear(Color.black);
  }

  @Override
  public void init() {
    Init.init();
    super.init();
  }

  @Override
  public void update() {
    super.update();
    graphics.clear(Color.black);

    // 资源加载完成后的初始化
    if (assets.update() && !assinited) {
      Fonts.setup();
      atlas = assets.get("sprites/sprites.aatls", TextureAtlas.class);
      Styles.load();
      UI.initAll();
      UI.Menu();
      UI.Debug();
      UnitControl unitCtrl = new UnitControl().init();
      camInput = new CameraInput().init();
      InputMultiplexer multiplexer =
          new InputMultiplexer(
              scene,
              new GestureDetector(unitCtrl),
              new GestureDetector(camInput),
              unitCtrl,
              camInput);
      input.addProcessor(multiplexer);
      addSystem(camInput);
      ContentVar.load();
      UI.camera.resize(graphics.getWidth(), graphics.getHeight());
      UI.camera.update();
      assinited = true;
      Scl.setProduct(1);
      
      Log.info("Game Inited");

      // 计算消耗时间
      long durationNanos = java.lang.System.nanoTime() - startTime;

      // 转换单位
      long durationMillis = durationNanos / 100_000_0; // 毫秒 (带小数)
      long durationMicros = durationNanos / 1000; // 微秒 (整数)


      Log.info(
          "Game inited - Using: "
              + String.format("%d ms / %d µs", durationMillis, durationMicros));
    }

    // 加载界面
    if (!assinited) {
      UI.Loading(assets.getProgress());
    } else {
      Draw.proj(camera);

      for (int i = 0; i < systems.size; i++) {
        caliniya.armavoke.system.System sys = systems.get(i);
        if (sys == null) {
          continue;
        }
        sys.update();
      }
      camera.update();
      Draw.flush();
    }
    scene.act();
    scene.draw();
  }

  public static void addSystem(caliniya.armavoke.system.System<?>... newSystems) {
    boolean added = false;
    for (caliniya.armavoke.system.System<?> s : newSystems) {
      if (s != null && !systems.contains(s)) {
        if (!s.inited) s.init();
        systems.add(s);
        added = true;
      } // TODO: 应不应该重复添加
    }
    if (added) {
      systems.sort();
    }
  }

  @Override
  public void add(ApplicationListener module) {
    super.add(module);
    if (module instanceof Loadable l) {
      assets.load(l);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    assets.dispose();
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    scene.resize(width, height);
    camera.resize(width, height);
  }

  @Override
  public void pause() {
    Events.fire(new EventType.GamePause(true));
    Log.info("[Application] Game Pause");
    super.pause();
  }

  @Override
  public void resume() {
    Events.fire(new EventType.GamePause(false));
    Log.info("[Application] Game Resume");
    super.resume();
  }
}
