package caliniya.vergvoke.type.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.vergvoke.base.api.TechNodeContent;
import caliniya.vergvoke.base.game.ContentType;
import caliniya.vergvoke.base.type.CType;

//这也要有绘制能力吗？应该会有吧
public class ItemType extends ContentType implements TechNodeContent {
  
  public TextureRegion icon;
  
  public ItemType(String name) {
    super(name, CType.Item);
  }

  @Override
  public TechNodeContent[] requirements() {
    return requirements; // ContentType 里的前置字段（默认 null）
  }
  
  public void load() {
    icon = Core.atlas.find(name);
  }
}
