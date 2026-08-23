package caliniya.armavoke.world.blocks.produce.recipe;

import caliniya.armavoke.type.Item;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.type.type.UnitType;

/** 单位工厂配方：产物、生产时间与一次性物品消耗。 */
public class Recipe {

  public final String name;
  public final UnitType output;
  public final float craftTimeSeconds;
  public final Item[] requirements;

  public Recipe(String name, UnitType output, float craftTimeSeconds, Item... requirements) {
    this.name = name;
    this.output = output;
    this.craftTimeSeconds = Math.max(0.01f, craftTimeSeconds);
    this.requirements = requirements == null ? new Item[0] : requirements;
  }

  public boolean canConsume(ItemModule module) {
    if (module == null) return requirements.length == 0;
    for (Item stack : requirements) {
      if (stack == null || stack.isEmpty()) continue;
      if (module.getAmount(stack.type) < stack.amount) return false;
    }
    return true;
  }

  /** 先完整校验再统一扣料，避免只扣掉一部分。 */
  public boolean consume(ItemModule module) {
    if (!canConsume(module)) return false;
    for (Item stack : requirements) {
      if (stack != null && !stack.isEmpty()) module.removeItem(stack.type, stack.amount);
    }
    return true;
  }

  public String requirementText() {
    if (requirements.length == 0) return "无消耗";
    StringBuilder out = new StringBuilder();
    for (Item stack : requirements) {
      if (stack == null || stack.isEmpty()) continue;
      if (out.length() > 0) out.append(" + ");
      out.append(stack.type.localizedName).append(" x").append(stack.amount);
    }
    return out.length() == 0 ? "无消耗" : out.toString();
  }
}
