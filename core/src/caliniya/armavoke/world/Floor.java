package caliniya.armavoke.world;

import arc.*;
import arc.graphics.g2d.*;
import caliniya.armavoke.base.game.*;
import caliniya.armavoke.base.type.*;

public class Floor extends ContentType {

  public TextureRegion region;

  public Floor(String name) {
    super(name, CType.Floor);
  }

  public void load() {
    region = Core.atlas.find(name);
  }
}
