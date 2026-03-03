package caliniya.armavoke.type.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;

//目前我还想不到该有什么其他东西(
public class ItemType extends ContentType {
  
  public TextureRegion icon;
  
  public ItemType(String name) {
    super(name, CType.Item);
  }
  
  public void load() {
  	icon = Core.atlas.find(name);
  }
  
}
