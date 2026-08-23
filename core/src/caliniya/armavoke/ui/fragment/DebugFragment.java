package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.ecs.runtime.EcsQueries;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.Unit;

/** ECS-only debug counters. */
public class DebugFragment {
  private Table root;

  public void add() {
    if (root != null) return;
    root = new Table();
    root.setFillParent(true);
    root.top().left().margin(8f);
    root.label(this::text).left();
    Core.scene.root.addChild(root);
  }

  private String text() {
    int width = WorldData.world == null ? 0 : WorldData.world.W;
    int height = WorldData.world == null ? 0 : WorldData.world.H;
    return "ECS world " + width + "x" + height
        + "\nunits: " + EcsQueries.count(Unit.class)
        + "\nbuildings: " + EcsQueries.count(Building.class)
        + "\nbullets: " + EcsQueries.count(Bullet.class)
        + "\ntotal: " + EcsQueries.snapshot().length;
  }

  public void remove() {
    if (root != null) root.remove();
    root = null;
  }
}
