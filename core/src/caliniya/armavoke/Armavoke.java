package caliniya.armavoke;

import arc.graphics.g2d.SpriteBatch;
import arc.input.InputMultiplexer;
import arc.input.GestureDetector;
import caliniya.armavoke.base.tool.Ar;
import arc.graphics.g2d.Draw;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.game.type.UnitType;
import caliniya.armavoke.system.BasicSystem;
import caliniya.armavoke.system.render.MapRender;
import caliniya.armavoke.system.input.*;
import caliniya.armavoke.ui.fragment.*;
import arc.ApplicationCore;
import arc.ApplicationListener;
import arc.assets.Loadable;
import arc.graphics.Color;
import arc.graphics.g2d.TextureAtlas;
import arc.util.Log;
import caliniya.armavoke.system.world.*;
import caliniya.armavoke.content.*;
import caliniya.armavoke.ui.*;
import static arc.Core.*;

public class Armavoke extends ApplicationCore {

  public boolean assinited = false;
  public CameraInput camInput;

  public static Ar<BasicSystem> systems = new Ar<BasicSystem>(10);

  @Override
  public void setup() {
    graphics.clear(Color.black);
    camera.resize(graphics.getWidth(), graphics.getHeight());
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
      UI.initAll();
      atlas = assets.get("sprites/sprites.aatls", TextureAtlas.class);
      Styles.load();
      UI.Menu();
      UI.Debug();
      scene.resize(graphics.getWidth(), graphics.getHeight());
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
      assinited = true;
    }

    // 加载界面
    if (!assinited) {
      UI.Loading();
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
      }// TODO: 应不应该重复添加
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
