package caliniya.armavoke.ecs.runtime;

import arc.math.Mathf;
import caliniya.armavoke.base.effect.Fx;
import caliniya.armavoke.ecs.generated.access.ConsumptionAccess;
import caliniya.armavoke.ecs.generated.access.ProductionAccess;
import caliniya.armavoke.ecs.generated.access.SpawnerAccess;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Item;
import caliniya.armavoke.world.blocks.produce.recipe.Recipe;
import caliniya.armavoke.world.blocks.produce.unit.FactoryBuild;

/** ECS production implementation for unit factories. */
public final class EcsFactoryRuntime {
  private EcsFactoryRuntime() {}

  public static boolean selectRecipe(FactoryBuild view, int index) {
    if (view == null || index < 0 || index >= view.factory().recipes.length) return false;
    EcsEntity entity = GameEcsBridge.ecs(view);
    if (entity instanceof ProductionAccess production) {
      if (production.productionComponent().crafting) return false;
      production.productionComponent().recipeId = index;
      production.productionComponent().progress = 0f;
      configure(entity, view, view.factory().recipes[index]);
    } else if (view.crafting) {
      return false;
    }
    view.recipeIndex = index;
    view.progress = 0f;
    return true;
  }

  public static boolean stopRecipe(FactoryBuild view) {
    if (view == null) return false;
    EcsEntity entity = GameEcsBridge.ecs(view);
    if (entity instanceof ProductionAccess production) {
      if (production.productionComponent().crafting) return false;
      production.productionComponent().recipeId = -1;
      production.productionComponent().progress = 0f;
    } else if (view.crafting) {
      return false;
    }
    view.recipeIndex = -1;
    view.progress = 0f;
    return true;
  }

  public static void update(EcsEntity entity, FactoryBuild view, float delta) {
    if (!(entity instanceof ProductionAccess production)) return;
    int recipeIndex = production.productionComponent().recipeId;
    if (recipeIndex < 0 || recipeIndex >= view.factory().recipes.length) {
      readComponentsToView(entity, view);
      return;
    }
    Recipe recipe = view.factory().recipes[recipeIndex];
    configure(entity, view, recipe);
    if (!production.productionComponent().crafting) {
      if (!recipe.consume(view.item)) {
        readComponentsToView(entity, view);
        return;
      }
      production.productionComponent().crafting = true;
      production.productionComponent().progress = 0f;
    } else if (production.productionComponent().progress
        >= production.productionComponent().craftTime) {
      spawn(view, recipe);
      production.productionComponent().progress = 0f;
      production.productionComponent().crafting = false;
      if (entity instanceof SpawnerAccess spawner) {
        spawner.spawnerOutputCount(spawner.spawnerOutputCount() + 1);
      }
    }
    readComponentsToView(entity, view);
  }

  public static void writeViewToComponents(EcsEntity entity, FactoryBuild view) {
    if (!(entity instanceof ProductionAccess production)) return;
    int index = view.recipeIndex;
    production.productionComponent().recipeId = index;
    production.productionComponent().crafting = view.crafting;
    Recipe recipe =
        index >= 0 && index < view.factory().recipes.length
            ? view.factory().recipes[index]
            : null;
    float craftTime = recipe == null ? 300f : craftTicks(recipe);
    production.productionComponent().craftTime = craftTime;
    production.productionComponent().progress = Mathf.clamp(view.progress) * craftTime;
    if (recipe != null) configure(entity, view, recipe);
  }

  public static void readComponentsToView(EcsEntity entity, FactoryBuild view) {
    if (!(entity instanceof ProductionAccess production)) return;
    view.recipeIndex = production.productionComponent().recipeId;
    view.crafting = production.productionComponent().crafting;
    float craftTime = Math.max(0.001f, production.productionComponent().craftTime);
    view.progress = Mathf.clamp(production.productionComponent().progress / craftTime);
  }

  private static void configure(EcsEntity entity, FactoryBuild view, Recipe recipe) {
    ProductionAccess production = (ProductionAccess) entity;
    production.productionComponent().craftTime = craftTicks(recipe);
    int totalItems = 0;
    for (Item item : recipe.requirements) {
      if (item != null && !item.isEmpty()) totalItems += item.amount;
    }
    if (entity instanceof ConsumptionAccess consumption) {
      consumption.consumptionItemCost(totalItems);
      consumption.consumptionPowerCost(0f);
      consumption.consumptionLiquidCost(0f);
    }
    if (entity instanceof SpawnerAccess spawner) {
      spawner.spawnerOutputTypeId(
          recipe.output == null ? -1 : recipe.output.internalName.hashCode());
    }
  }

  private static float craftTicks(Recipe recipe) {
    return recipe.craftTimeSeconds * 60f;
  }

  private static void spawn(FactoryBuild factory, Recipe recipe) {
    float distance = factory.factory().psize / 2f + recipe.output.size / 2f + 8f;
    float spawnX = factory.x;
    float spawnY = factory.y;
    switch (factory.angle & 3) {
      case 0 -> spawnY += distance;
      case 1 -> spawnX += distance;
      case 2 -> spawnY -= distance;
      case 3 -> spawnX -= distance;
      default -> {
      }
    }
    float maxX = WorldData.world.W * WorldData.TILE_SIZE;
    float maxY = WorldData.world.H * WorldData.TILE_SIZE;
    spawnX = Mathf.clamp(spawnX, 0f, maxX);
    spawnY = Mathf.clamp(spawnY, 0f, maxY);
    EcsUnitRuntime.create(factory.team, recipe.output, spawnX, spawnY);
    Fx.spawn.at(spawnX, spawnY, recipe.output);
  }
}
