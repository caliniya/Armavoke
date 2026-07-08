package caliniya.armavoke.system.render;

import static arc.Core.*;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import caliniya.armavoke.core.meta.ui.Pal;
import caliniya.armavoke.ui.Button;

public class DebugRender extends caliniya.armavoke.system.System<DebugRender> {

  @Override
  public DebugRender init() {
    index = 14;
    return super.init(false);
  }

  @Override
  public void update() {
    for (Element e : scene.root.getChildren()) {
      if (e.fillParent) continue;
      Lines.stroke(2f, Pal.light);
      Lines.rect(e.x, e.y, e.getWidth(), e.getHeight());
    }
  }
}
