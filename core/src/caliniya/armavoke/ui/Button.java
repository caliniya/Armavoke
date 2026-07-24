package caliniya.armavoke.ui;

import arc.*;
import arc.scene.style.*;
import arc.util.*;
import arc.scene.ui.*;
import caliniya.armavoke.base.type.*;

public class Button extends ImageButton {
  public Button(String text, Runnable action) {
    super();
    clicked(action);
    add(text).growX().scrollX(true).center().get().setAlignment(Align.center, Align.center);
    setStyle(Styles.buttondef);
  }

  public Button(Runnable action, String text) {
    super();
    clicked(action);
    add(text).growX().scrollX(true).center().get().setAlignment(Align.center, Align.center);
    setStyle(Styles.buttonc);
  }
}
