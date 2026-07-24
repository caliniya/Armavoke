package caliniya.armavoke;

import arc.backend.sdl.*;
import arc.backend.sdl.*;
import arc.graphics.gl.*;
import caliniya.armavoke.*;

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
