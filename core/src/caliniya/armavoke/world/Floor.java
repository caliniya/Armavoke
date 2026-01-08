package caliniya.armavoke.world;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;

public class Floor extends ContentType {

  public TextureRegion region;

  public Floor(String name) {
    super(name, CType.Floor);
  }

  public void load() {

    region = Core.atlas.find(name);

    if (!Core.atlas.isFound(region)) {
      // region = Core.atlas.find("error");
    }
  }
}
