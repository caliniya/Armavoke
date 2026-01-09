package caliniya.armavoke.world;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;

public class ENVBlock extends ContentType {
  
    public TextureRegion region;

    public ENVBlock(String name){
        super(name, CType.ENVBlock);
    }
    
    public void load() {
        region = Core.atlas.find(name);
    }
}