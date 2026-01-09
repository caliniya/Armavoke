package caliniya.armavoke.world;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.world.*;

public class Block extends ContentType {

  public float size = 2;
  public boolean buildable = true;
  public float health = 10;
  
  public TextureRegion region;

  public Block(String BlockName) {
    super(BlockName, CType.Block);
  }
  
  public void load() {
    region = Core.atlas.find(name);
  }
  
  public boolean isMultiblock() {
    return size == 1;
  }
  
  /**
  public static Block Creat() {
    return new Block(name);
  }
  */
}
