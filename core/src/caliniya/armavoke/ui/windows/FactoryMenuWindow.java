package caliniya.armavoke.ui.windows;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import caliniya.armavoke.game.Contents;
import caliniya.armavoke.type.type.ItemType;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.world.blocks.produce.recipe.Recipe;
import caliniya.armavoke.world.blocks.produce.unit.FactoryBuild;

/** 通用单位工厂菜单，实时显示配方、库存、进度、电力和液体状态。 */
public class FactoryMenuWindow extends Window {

  private final FactoryBuild factory;

  public FactoryMenuWindow(FactoryBuild factory) {
    super(factory.factory().localizedName);
    this.factory = factory;
  }

  @Override
  public void main(Table table) {
    table.add("[light]配方列表[]").growX().left().row();
    Recipe[] recipes = factory.factory().recipes;
    for (int i = 0; i < recipes.length; i++) {
      int index = i;
      Recipe recipe = recipes[i];
      String text =
          recipe.name
              + "\n"
              + recipe.requirementText()
              + " / "
              + Strings.fixed(recipe.craftTimeSeconds, 1)
              + "秒";
      table
          .add(
              new Button(
                  text,
                  () -> {
                    factory.selectRecipe(index);
                  }))
          .growX()
          .height(72f)
          .pad(4f)
          .row();
    }

    table.add(new Button("停止队列", factory::stopRecipe)).growX().height(48f).pad(4f).row();
    table.add().height(8f).row();

    Label live =
        new Label("") {
          @Override
          public void act(float delta) {
            super.act(delta);
            setText(statusText());
          }
        };
    live.setWrap(true);
    table.add(live).growX().left().pad(6f);
  }

  private String statusText() {
    StringBuilder out = new StringBuilder();
    if (factory.health <= 0f) return "[red]工厂已失效[]";

    if (factory.recipeIndex >= 0 && factory.recipeIndex < factory.factory().recipes.length) {
      Recipe recipe = factory.factory().recipes[factory.recipeIndex];
      out.append("当前配方: ").append(recipe.name).append('\n');
      out.append(factory.crafting ? "生产中: " : "等待材料: ")
          .append(Strings.fixed(factory.progress * 100f, 1))
          .append("%\n");
    } else {
      out.append("当前配方: 未选择\n");
    }

    out.append("\n物品库存:\n");
    boolean any = false;
    if (Contents.items != null) {
      for (ItemType item : Contents.items) {
        int amount = factory.item == null ? 0 : factory.item.getAmount(item);
        if (amount > 0) {
          any = true;
          out.append("  ").append(item.localizedName).append(": ").append(amount).append('\n');
        }
      }
    }
    if (!any) out.append("  空\n");

    if (factory.power == null) {
      out.append("电力: 无模块\n");
    } else {
      out.append("电力: ")
          .append(Strings.fixed(factory.power.power, 1))
          .append(" / ")
          .append(Strings.fixed(factory.power.powerMax, 1))
          .append('\n');
    }

    if (factory.liquid == null) {
      out.append("液体: 无模块");
    } else {
      out.append("液体: ")
          .append(Strings.fixed(factory.liquid.total(), 1))
          .append(" / ")
          .append(Strings.fixed(factory.liquid.capacity, 1));
    }
    return out.toString();
  }
}
