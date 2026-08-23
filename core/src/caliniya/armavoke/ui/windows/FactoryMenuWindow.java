package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.Table;
import caliniya.armavoke.ecs.runtime.EcsFactoryRuntime;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.world.blocks.produce.recipe.Recipe;
import caliniya.armavoke.world.blocks.produce.unit.Factory;

/** Factory controls for a normal ECS Building entity. */
public class FactoryMenuWindow extends Window {
  private final Building factory;

  public FactoryMenuWindow(Building factory) {
    super("单位工厂");
    this.factory = factory;
  }

  @Override
  public void main(Table table) {
    if (factory == null || !(factory.block() instanceof Factory type)) {
      table.add("[red]工厂实体已失效[]");
      return;
    }
    table.defaults().growX().pad(4f);
    table.label(() -> status(type)).left().row();
    for (int i = 0; i < type.recipes.length; i++) {
      Recipe recipe = type.recipes[i];
      int index = i;
      table.button(recipe.name + "  " + recipe.requirementText(), () -> EcsFactoryRuntime.selectRecipe(factory, index)).row();
    }
    table.button("停止生产", () -> EcsFactoryRuntime.stopRecipe(factory)).row();
  }

  private String status(Factory type) {
    if (!factory.active() || factory.health() <= 0f) return "[red]工厂已失效[]";
    int index = EcsFactoryRuntime.recipeIndex(factory);
    String recipe = index >= 0 && index < type.recipes.length ? type.recipes[index].name : "无";
    return "配方: " + recipe + "  进度: " + (int) (EcsFactoryRuntime.progress(factory) * 100f) + "%";
  }
}
