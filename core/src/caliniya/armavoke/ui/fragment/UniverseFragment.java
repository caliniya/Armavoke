package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.event.EventListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.system.render.UniverseRender;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.ui.Styles;
import caliniya.armavoke.world.stars.Universe;

/** */
public class UniverseFragment {

  public Table root;
  private final Vec2 world = new Vec2();

  public void build() {
    root = new Table();
    root.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(
              InputEvent event, float x, float y, int pointer, KeyCode button) {
            // 只在命中根 Table 背景时处理；菜单按钮由它们自己的 listener 消费，不会到这儿
            if (event.targetActor == root || event.targetActor == null) {
              updateSelection(event.stageX, event.stageY);
              return true; // 消费事件，阻止穿透到下层输入处理器
            }
            return false;
          }

          @Override
          public void touchDragged(InputEvent event, float x, float y, int pointer) {
            updateSelection(x, y);
          }

          @Override
          public boolean mouseMoved(InputEvent event, float x, float y) {
            updateSelection(x, y);
            return false;
          }
        });
    root.setFillParent(true);
    root.setBackground(Styles.background);
    root.touchable = Touchable.enabled;

    root.add(
        new Button(
            "关闭",
            () -> {
              root.remove();
              UI.hud.showHUD();
              UI.pauseWindow.window.visible = true;
              Events.fire(EventType.events.ExitUV);
            }));

    Core.scene.root.addChild(root);
    Events.fire(EventType.events.EnterUV);
  }

  private void updateSelection(float x, float y) {
    world.set(x, y);
    Render.universeCamera.unproject(world);

    float gs = UniverseRender.GRID_SIZE;
    Universe.selectedX = (float) Math.floor(world.x / gs) * gs;
    Universe.selectedY = (float) Math.floor(world.y / gs) * gs;
    Universe.hasSelection = true;
  }
}
