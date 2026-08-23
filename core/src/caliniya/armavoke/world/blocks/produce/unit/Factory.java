package caliniya.armavoke.world.blocks.produce.unit;

import caliniya.armavoke.base.effect.Fx;
import arc.Core;
import arc.math.Mathf;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.world.Block;
import caliniya.armavoke.world.blocks.produce.recipe.Recipe;

/** 可配置多份单位配方的通用工厂方块。 */
public class Factory extends Block {

  public Recipe[] recipes = new Recipe[0];
  public String regionName = "test-building";

  public Factory(String name) {
    super(name);
  }

  public Factory recipes(Recipe... recipes) {
    this.recipes = recipes == null ? new Recipe[0] : recipes;
    return this;
  }

  @Override
  public FactoryBuild create() {
    psize = size * WorldData.TILE_SIZE;
    return FactoryBuild.create(this);
  }

  @Override
  public FactoryBuild create(int tx, int ty, TeamTypes team) {
    psize = size * WorldData.TILE_SIZE;
    return FactoryBuild.create(this, tx, ty, 0, team);
  }

  @Override
  public void load() {
    region = Core.atlas.find(regionName, "white");
  }

  @Override
  public void update(Building building, float dt) {
    if (!(building instanceof FactoryBuild factory)) return;
    if (factory.recipeIndex < 0 || factory.recipeIndex >= recipes.length) return;

    Recipe recipe = recipes[factory.recipeIndex];
    if (!factory.crafting) {
      if (!recipe.consume(factory.item)) return;
      factory.crafting = true;
      factory.progress = 0f;
    }

    factory.progress += dt / (recipe.craftTimeSeconds * 60f);
    if (factory.progress >= 1f) {
      spawn(factory, recipe);
      factory.progress = 0f;
      factory.crafting = false;
    }
  }

  private void spawn(FactoryBuild factory, Recipe recipe) {
    float distance = psize / 2f + recipe.output.size / 2f + 8f;
    float spawnX = factory.x;
    float spawnY = factory.y;
    switch (factory.angle & 3) {
      case 0 -> spawnY += distance;
      case 1 -> spawnX += distance;
      case 2 -> spawnY -= distance;
      case 3 -> spawnX -= distance;
    }

    float maxX = WorldData.world.W * WorldData.TILE_SIZE;
    float maxY = WorldData.world.H * WorldData.TILE_SIZE;
    spawnX = Mathf.clamp(spawnX, 0f, maxX);
    spawnY = Mathf.clamp(spawnY, 0f, maxY);
    recipe.output.create(factory.team, spawnX, spawnY);
    Fx.spawn.at(spawnX, spawnY, recipe.output);
  }

  @Override
  public void write(Building building, Writes writes) {
    FactoryBuild factory = (FactoryBuild) building;
    writes.i(factory.recipeIndex);
    writes.f(factory.progress);
    writes.bool(factory.crafting);
  }

  @Override
  public void read(Building building, Reads reads) {
    FactoryBuild factory = (FactoryBuild) building;
    factory.recipeIndex = reads.i();
    factory.progress = reads.f();
    factory.crafting = reads.bool();
  }
}
