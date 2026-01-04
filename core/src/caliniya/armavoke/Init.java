package caliniya.armavoke;

import arc.*;
import arc.assets.AssetManager;
import arc.assets.loaders.I18NBundleLoader;
import arc.graphics.Camera;
import arc.graphics.Texture;
import arc.graphics.g2d.SpriteBatch;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureRegion;
import arc.math.Scaled;
import arc.scene.Scene;
import arc.util.viewport.ScreenViewport;
import arc.util.*;

import caliniya.armavoke.core.InitGame;
import java.util.*;

import caliniya.armavoke.core.UI;
import caliniya.armavoke.ui.*;

import static arc.Core.*;

public class Init {

  public static boolean android = app.isAndroid();
  public static boolean desktop = app.isDesktop();

  public static boolean inited;

  public static Locale locale = Locale.getDefault();

  @SuppressWarnings("unused")
  public static void init() {
    // assets.load("");
    inited = false;

    settings.setAppName("Armavoke");

    if (desktop) {
      // TODO: 在这里实现桌面端的日志处理器，但是桌面端长什么样?
    }

    // 基本平台信息
    Log.info("Graphics init");
    Log.infoTag("Init", "[GL] Version:" + graphics.getGLVersion());
    Log.info("[Init] [GL] Using " + (gl30 != null ? "OpenGL 3" : "OpenGL 2"));
    if (gl30 == null) {
      Log.warn(
          "[Waning] device or video drivers do not support OpenGL 3. This will cause performance issues.");
    }
    long ram = Runtime.getRuntime().maxMemory();
    boolean gb = ram >= 1024 * 1024 * 1024;
    Log.info(
        "[RAM] Available: @ @",
        Strings.fixed(gb ? ram / 1024f / 1024 / 1024f : ram / 1024f / 1024f, 1),
        gb ? "GB" : "MB");

    bundle = I18NBundle.createBundle(files.internal("language/language"), locale);
    assets = new AssetManager();
    camera = new Camera();
    scene = new Scene(new ScreenViewport(new Camera()));
    batch = new SpriteBatch();
    input.addProcessor(scene);
    Fonts.loadSystem();
    Fonts.loadFonts();
    Log.info("inited basic system");

    if (assets == null) {
      Log.info("init assets(unexpected)");
      assets = new AssetManager();
    }

    assets.load("sprites/white.png", Texture.class);
    assets.finishLoading();
    // 在这里阻塞加载让加载界面能用
    atlas = new TextureAtlas();
    atlas.addRegion("white", assets.get("sprites/white.png"), 1, 1, 1, 1);

    assets.load("sprites/sprites.aatls", TextureAtlas.class);
    inited();
  }

  public static void inited() {
    inited = true;
    Log.info("Inited");
  }
}
