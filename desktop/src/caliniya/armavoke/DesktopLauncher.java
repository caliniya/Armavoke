package caliniya.armavoke;

import arc.backend.sdl.SdlApplication;
import arc.backend.sdl.SdlConfig;
import arc.graphics.gl.HdpiMode;
import caliniya.armavoke.Armavoke;

public class DesktopLauncher {
  public static void main(String[] arg) {
    new SdlApplication(new Armavoke(), new SdlConfig() {
      {
        title = "armavoke";
        maximized = true;
        width = 900;
        height = 700;
      }
    });
  }
}
