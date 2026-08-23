package caliniya.armavoke.ecs.runtime;

import arc.math.Mathf;
import caliniya.armavoke.ecs.definition.GameComponents;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.world.blocks.produce.recipe.Recipe;
import caliniya.armavoke.world.blocks.produce.unit.Factory;

/** Factory production operating solely on Building ECS components. */
public final class EcsFactoryRuntime {
  private EcsFactoryRuntime() {}

  public static boolean selectRecipe(Building building, int index) {
    Factory factory = factory(building);
    if (factory == null || index < 0 || index >= factory.recipes.length) return false;
    Recipe recipe = factory.recipes[index];
    if (recipe == null || !recipe.consume(building.item())) return false;
    GameComponents.Production production = building.productionComponent();
    production.recipeId = index;
    production.progress = 0f;
    production.craftTime = Math.max(1f, recipe.craftTimeSeconds * 60f);
    production.crafting = true;
    building.spawnerOutputTypeId(recipe.output == null ? -1 : recipe.output.id);
    return true;
  }

  public static boolean stopRecipe(Building building) {
    if (factory(building) == null) return false;
    GameComponents.Production production = building.productionComponent();
    production.crafting = false;
    production.progress = 0f;
    production.recipeId = -1;
    return true;
  }

  public static void update(Building building, float delta) {
    Factory factory = factory(building);
    if (factory == null) return;
    GameComponents.Production production = building.productionComponent();
    if (!production.crafting || production.recipeId < 0 || production.recipeId >= factory.recipes.length) return;
    Recipe recipe = factory.recipes[production.recipeId];
    production.progress += delta;
    if (production.progress < production.craftTime) return;
    production.progress = 0f;
    if (recipe.output != null) {
      float distance = Math.max(32f, building.size() * 0.75f);
      float x = building.x() + Mathf.cosDeg(building.angle() * 90f) * distance;
      float y = building.y() + Mathf.sinDeg(building.angle() * 90f) * distance;
      EcsEntityFactory.createUnit(recipe.output, building.team(), x, y);
      building.spawnerOutputCount(building.spawnerOutputCount() + 1);
    }
    if (!recipe.consume(building.item())) {
      production.crafting = false;
      production.recipeId = -1;
    }
  }

  public static float progress(Building building) {
    GameComponents.Production production = building.productionComponent();
    return production.craftTime <= 0f ? 0f : Mathf.clamp(production.progress / production.craftTime);
  }

  public static int recipeIndex(Building building) { return building.productionComponent().recipeId; }
  public static boolean crafting(Building building) { return building.productionComponent().crafting; }

  private static Factory factory(Building building) {
    return building != null && building.block() instanceof Factory value ? value : null;
  }
}
