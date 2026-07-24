package caliniya.armavoke.type;

import arc.util.io.*;
import arc.util.io.*;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.type.type.*;

public class Item {
  
  public ItemType type;
  public int amount;

  public Item(ItemType type, int amount) {
    this.type = type;
    this.amount = amount;
  }

  public void write(Writes write) {
    write.s(type == null ? -1 : type.id);
    write.i(amount);
  }

  public void read(Reads read) {
    short id = read.s();
    this.type = Contents.getByID(CType.Item, id);
    this.amount = read.i();
  }
  
  public void set(ItemType type, int amount) {
    this.type = type;
    this.amount = amount;
  }
  
  public boolean isEmpty() {
    return type == null || amount <= 0;
  }
}