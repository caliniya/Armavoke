package caliniya.armavoke.base.type;

import caliniya.armavoke.base.game.*;
import caliniya.armavoke.game.type.*;
import caliniya.armavoke.world.*;
import caliniya.armavoke.type.type.*;

public enum CType {
  Block(Block.class),
  Floor(Floor.class),
  ENVBlock(ENVBlock.class),
  Unit(UnitType.class),
  Item(ItemType.class);
  
  
  public final Class<? extends ContentType> type;

  CType(Class<? extends ContentType> contentClass) {
    this.type = contentClass;
  }
}