package caliniya.armavoke.system.render;

import arc.Core;
import arc.Events;
import arc.graphics.Camera;
import arc.graphics.g2d.Draw;
import arc.util.Log;
import caliniya.armavoke.base.shaders.SpaceShader;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.system.System;
import caliniya.armavoke.system.input.UniverseCameraInput;

/** 宇宙渲染 */
public class UniverseRender extends System<UniverseRender> {

  /** 太空背景着色器 */
  private SpaceShader background;

  @Override
  public UniverseRender init() {
    this.index = 16;
    background = new SpaceShader(Render.universeCamera, () -> UniverseCameraInput.zoom);
    background.parallaxScale = 0.5f;
    background.baseScale = 0.6f;
    Events.run(EventType.events.EnterUV, () -> paused = false);
    Events.run(EventType.events.ExitUV, () -> paused = true);
    paused = true;
    return super.init(false, false);
  }

  @Override
  public void update() {
    if (!inited || paused) return;
    Camera cam = Render.universeCamera;
    float zoom = UniverseCameraInput.zoom;
    background.render();
    Draw.proj(cam);
    Game.starMap.draw(cam);
    Log.info(cam.mat);
    Draw.proj(Core.camera);
  }

  @Override
  public void dispose() {
    if (background != null) background.dispose();
    super.dispose();
  }
}
