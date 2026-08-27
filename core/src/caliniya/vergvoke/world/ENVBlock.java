package caliniya.vergvoke.world;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.vergvoke.base.game.ContentType;
import caliniya.vergvoke.base.type.CType;

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
