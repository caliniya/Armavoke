package caliniya.armavoke;

import arc.Core;
import static arc.Core.*;

import arc.ApplicationCore;
import arc.ApplicationListener;
import arc.assets.Loadable;
import arc.graphics.Camera;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.SpriteBatch;
import arc.graphics.g2d.TextureAtlas;
import arc.input.*;
import arc.scene.Scene;
import arc.util.Log;
import arc.util.viewport.ScreenViewport;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.content.*;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.game.type.UnitType;
import caliniya.armavoke.system.*;
import caliniya.armavoke.system.input.*;
import caliniya.armavoke.system.render.*;
import caliniya.armavoke.system.world.*;
import caliniya.armavoke.ui.*;
import caliniya.armavoke.ui.Styles;
import caliniya.armavoke.ui.fragment.*;

public class Armavoke extends ApplicationCore {

  public boolean assinited = false;
  public CameraInput camInput;

  public static Ar<BasicSystem> systems = new Ar<BasicSystem>(10);

  @Override
  public void setup() {
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
      Log.info("loaded");
      InputMultiplexer multiplexer =
          new InputMultiplexer(
              scene,
              new GestureDetector(unitCtrl),
              new GestureDetector(camInput),
              unitCtrl,
              camInput);
      input.addProcessor(multiplexer);
      addSystem(camInput);
      UnitTypes.load();
      Floors.load();
      ENVBlocks.load();
      UI.camera.resize(graphics.getWidth(),graphics.getHeight());
      UI.camera.update();
      assinited = true;
    }

    // 加载界面
    if (!assinited) {
      UI.Loading(assets.getProgress());
    } else {
      Draw.proj(camera);

      for (int i = 0; i < systems.size; i++) {
        BasicSystem sys = systems.get(i);
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

  public static void addSystem(BasicSystem<?>... newSystems) {
    boolean added = false;
    for (BasicSystem<?> s : newSystems) {
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
}
