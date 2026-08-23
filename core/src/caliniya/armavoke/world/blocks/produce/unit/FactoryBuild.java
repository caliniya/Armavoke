package caliniya.armavoke.world.blocks.produce.unit;

import arc.util.pooling.Pools;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.ecs.runtime.EcsFactoryRuntime;
import caliniya.armavoke.type.Building;

/** 单位工厂的运行时建筑状态。 */
public class FactoryBuild extends Building {

  public int recipeIndex = -1;
  public float progress;
  public boolean crafting;

  protected FactoryBuild() {}

  public Factory factory() {
    return (Factory) block;
  }

  public boolean selectRecipe(int index) {
    return EcsFactoryRuntime.selectRecipe(this, index);
  }

  public boolean stopRecipe() {
    return EcsFactoryRuntime.stopRecipe(this);
  }

  @Override
  public void reset() {
    super.reset();
    recipeIndex = -1;
    progress = 0f;
    crafting = false;
    item = null;
    liquid = null;
    power = null;
    block = null;
  }

  public static FactoryBuild create(
      Factory factory, int tx, int ty, int angle, TeamTypes team) {
    FactoryBuild building = Pools.obtain(FactoryBuild.class, FactoryBuild::new);
    building.block = factory;
    building.tx = tx;
    building.ty = ty;
    building.angle = angle;
    building.team = team;
    building.teamData = team.data();
    building.init();
    building.id = Entities.assignID();
    return building;
  }

  public static FactoryBuild create(Factory factory) {
    FactoryBuild building = Pools.obtain(FactoryBuild.class, FactoryBuild::new);
    building.block = factory;
    building.init();
    return building;
  }
}
