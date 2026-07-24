package caliniya.armavoke;

import static arc.Core.*;

import android.os.*;
import arc.*;
import arc.*;
import arc.backend.android.*;
import arc.backend.android.*;
import arc.files.*;
import arc.util.*;
import arc.util.Log.*;
import cat.ereza.customactivityoncrash.config.*;
import java.io.*;

public class AndroidLauncher extends AndroidApplication {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    CaocConfig.Builder.create()
        .enabled(true)
        .errorActivity(ErrorActivity.class)
        .apply();

    initialize(new Armavoke(),
        new AndroidApplicationConfiguration() {
          {
            useImmersiveMode = true;
            hideStatusBar = true;
            useGL30 = true;
          }
        });
    Fi data = Core.files.absolute(this.getExternalFilesDir(null).getAbsolutePath());
    // throw new ArcRuntimeException(data.toString());
    Core.settings.setDataDirectory(data);
    try {
      Writer writer = settings.getDataDirectory().child("log.txt").writer(false);
      LogHandler originalLogger = Log.logger;
      // 要过滤的标签列表(它们太多了而且一般没有用)
      String[] filteredTags = { "AndroidGraphics", "GL30", "OtherTag" };

      Log.logger = (level, text) -> {
        if (level == LogLevel.info && Log.level.ordinal() > LogLevel.debug.ordinal()) {
          for (String tag : filteredTags) {
            if (text.matches("\\[" + tag + "\\].*")) {
              return;
            }
          }
        }
        originalLogger.log(level, text);
        try {
          writer.write("[" + Character.toUpperCase(level.name().charAt(0)) + "] " +
              Log.removeColors(text) + "\n");
          writer.flush();
        } catch (Exception e) {
          e.printStackTrace();
        }
      };
    } catch (Exception e) {
      //只能这么做了
      Log.err(e);
    }
    Log.level = Log.LogLevel.info;
    Log.info("start-Android");

  }

  @Override
  public void addListener(ApplicationListener appl) {
    synchronized (this.getListeners()) {
      this.getListeners().add(appl);
    }
  }
}
