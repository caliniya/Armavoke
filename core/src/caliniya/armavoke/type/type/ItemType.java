package caliniya.armavoke.type.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;

//这也要有绘制能力吗？应该会有吧
public class ItemType extends ContentType {
  
  public TextureRegion icon;
  
  public ItemType(String name) {
    super(name, CType.Item);
  }
  
  public void load() {
    icon = Core.atlas.find(name);
  }
}