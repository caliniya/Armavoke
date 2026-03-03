package caliniya.armavoke.type;

import caliniya.armavoke.type.type.*;

// 表示一个物品堆栈
public class Item {
  
  public ItemType type;
  public int amount;
  
  public Item(ItemType type, int amount) {
    this.type = type;
    this.amount = amount;
  }
}
