package caliniya.armavoke.world.blocks.produce.unit;

import arc.Core;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.world.Block;
import caliniya.armavoke.world.blocks.produce.recipe.Recipe;

/** Factory content definition. Production state lives in Building ECS components. */
public class Factory extends Block {
  public Recipe[] recipes = new Recipe[0];
  public String regionName = "test-building";

  public Factory(String name) { super(name); }

  public Factory recipes(Recipe... values) {
    recipes = values == null ? new Recipe[0] : values;
    return this;
  }

  @Override public Building create() { return super.create(); }
  @Override public Building create(int tx, int ty, TeamTypes team) { return super.create(tx, ty, team); }

  @Override
  public void load() {
    super.load();
    region = Core.atlas.find(regionName);
  }

  @Override public void update(Building building, float delta) {}
  @Override public void write(Building building, Writes writes) {}
  @Override public void read(Building building, Reads reads) {}
}
