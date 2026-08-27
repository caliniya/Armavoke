package caliniya.vergvoke;

import arc.backend.sdl.SdlApplication;
import arc.backend.sdl.SdlConfig;
import arc.graphics.gl.HdpiMode;
import caliniya.vergvoke.Vergvoke;

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
