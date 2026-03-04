package caliniya.armavoke.type.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;

public class ItemType extends ContentType {
  
  public TextureRegion icon;
  // 移除了 maxStack，具体容量由 ItemModule 决定
  
  public ItemType(String name) {
    super(name, CType.Item);
  }
  
  public void load() {
    icon = Core.atlas.find(name);
  }
}