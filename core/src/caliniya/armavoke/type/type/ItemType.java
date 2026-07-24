package caliniya.armavoke.type.type;

import arc.*;
import arc.graphics.g2d.*;
import caliniya.armavoke.base.game.*;
import caliniya.armavoke.base.type.*;

public class ItemType extends ContentType {
  
  public TextureRegion icon;
  
  public ItemType(String name) {
    super(name, CType.Item);
  }
  
  public void load() {
    icon = Core.atlas.find(name);
  }
}