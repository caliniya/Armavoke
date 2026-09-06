package caliniya.vergvoke;

import arc.backend.sdl.SdlApplication;
import arc.backend.sdl.SdlConfig;

public class DesktopLauncher {
  public static void main(String[] arg) {
    new SdlApplication(new Vergvoke(), new SdlConfig() {
      {
        title = "Vergvoke";
        maximized = true;
        width = 900;
        height = 700;
      }
    });
  }
}
