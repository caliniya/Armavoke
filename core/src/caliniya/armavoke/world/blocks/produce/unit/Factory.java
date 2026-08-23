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
    // Production is updated by the generated ECS general system.
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
