package caliniya.armavoke.ecs.runtime;

public interface EcsSystem {
  default void initialize(EcsWorld world) {}

  void update(EcsWorld world, float delta);

  default void dispose(EcsWorld world) {}
}
