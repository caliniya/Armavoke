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

  public void build() {
    root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.enabled;

    root.add(
        new Button(
            "关闭",
            () -> {
              root.remove();
              UI.hud.showHUD();
              UI.pauseWindow.window.visible = true;

              UI.pauseWindow.modalOverlay.visible = true;
              Events.fire(EventType.events.ExitUV);
            }));

    Core.scene.root.addChild(root);
    Events.fire(EventType.events.EnterUV);
  }
}
