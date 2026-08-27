package caliniya.vergvoke.world;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.vergvoke.base.game.ContentType;
import caliniya.vergvoke.base.type.CType;

public class Floor extends ContentType {

  public TextureRegion region;

  public Floor(String name) {
    super(name, CType.Floor);
  }

  public void load() {
    region = Core.atlas.find(name);
  }
}
