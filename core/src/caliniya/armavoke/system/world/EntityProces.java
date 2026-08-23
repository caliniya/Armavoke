package caliniya.armavoke.system.world;

import arc.struct.ObjectIntMap;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.ecs.runtime.EcsGameRuntime;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.Floor;

/** Compatibility facade. Targeting and persistence are now ECS systems. */
@Deprecated
public class EntityProces extends caliniya.armavoke.system.System<EntityProces> {
  public volatile boolean task2;
  public volatile boolean task3;
  public Ar<Floor> floorPalette;
  public ObjectIntMap<Floor> floorMap;
  public Ar<ENVBlock> blockPalette;
  public ObjectIntMap<ENVBlock> blockMap;

  @Override
  public EntityProces init() {
    return super.init(false);
  }

  @Override
  public void update() {
    if (Systems.ECS != null && Systems.ECS.world() != null) {
      EcsGameRuntime.updateTargeting(Systems.ECS.world(), 1f);
    }
  }
}
