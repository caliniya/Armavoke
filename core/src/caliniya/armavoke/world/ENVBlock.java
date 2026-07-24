package caliniya.armavoke.world;

import arc.*;
import arc.graphics.g2d.*;
import caliniya.armavoke.base.game.*;
import caliniya.armavoke.base.type.*;

public class ENVBlock extends ContentType {

  public TextureRegion region;
  
  public boolean solid = true;

  public ENVBlock(String name) {
    super(name, CType.ENVBlock);
  }

  public void load() {
    region = Core.atlas.find(name);
  }
}
