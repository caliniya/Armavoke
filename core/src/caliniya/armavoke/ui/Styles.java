package caliniya.armavoke.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.ScrollPane.ScrollPaneStyle;
import caliniya.armavoke.ui.Fonts;

public class Styles {
  
  public static ImageButtonStyle ibuttondef;
  public static LabelStyle labeldef;
  public static ScrollPaneStyle scrollf;

  public static void load() {
    
    ibuttondef = new ImageButtonStyle();
    ibuttondef.up = Core.atlas.drawable("button"); // 默认状态
    ibuttondef.down = Core.atlas.drawable("button-1"); // 按下状态
    ibuttondef.checked = Core.atlas.drawable("button-1");
    Core.scene.addStyle(ImageButtonStyle.class , ibuttondef);
    
    labeldef = new LabelStyle(Fonts.def , Color.white);
    Core.scene.addStyle(LabelStyle.class , labeldef);
    scrollf = new ScrollPaneStyle();
    Core.scene.addStyle(ScrollPaneStyle.class , scrollf);
  }
}
