package caliniya.armavoke.ui;

import arc.Events;
import arc.scene.style.Drawable;
import arc.util.Align;
import arc.scene.ui.ImageButton;
import caliniya.armavoke.base.type.EventType;

public class Button extends ImageButton {
  public Button(String text, Runnable action) {
    super();
    clicked(action);
    add(text).growX().scrollX(true).center().get().setAlignment(Align.center,Align.center);
    setStyle(Styles.ibuttondef);
  }
}
