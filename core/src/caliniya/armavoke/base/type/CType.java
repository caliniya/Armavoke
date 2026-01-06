package caliniya.armavoke.base.type;

import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.game.type.UnitType;
import caliniya.armavoke.world.Block;
import caliniya.armavoke.world.Floor;
import caliniya.armavoke.world.ENVBlock;

public enum CType {
  Block(Block.class),
  Floor(Floor.class),
  ENVBlock(ENVBlock.class),
  Unit(UnitType.class);
  
  public final Class<? extends ContentType> type;

  CType(Class<? extends ContentType> contentClass) {
    this.type = contentClass;
  }
}