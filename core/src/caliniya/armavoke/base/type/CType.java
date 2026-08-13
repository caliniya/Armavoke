package caliniya.armavoke.base.type;

import caliniya.armavoke.base.game.*;
import caliniya.armavoke.world.*;
import caliniya.armavoke.world.stars.*;
import caliniya.armavoke.type.enhance.EnhancementType;
import caliniya.armavoke.type.type.*;

public enum CType {
  Block(Block.class),
  Floor(Floor.class),
  ENVBlock(ENVBlock.class),
  Unit(UnitType.class),
  Item(ItemType.class),
  Liquid(LiquidType.class),
  StarNode(StarNode.class),
  Enhance(EnhancementType.class);


  public final Class<? extends ContentType> type;

  CType(Class<? extends ContentType> contentClass) {
    this.type = contentClass;
  }
}
