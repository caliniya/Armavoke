package caliniya.armavoke.ui.windows;
import arc.Core;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.ui.fragment.HUDFragment;
import caliniya.armavoke.ui.fragment.UniverseFragment;
import caliniya.armavoke.core.UI;

public class PauseWindow extends Window{
  /**
   * 呃啊
   */
  public PauseWindow(){
    super("@pauseWindow");
    w = Core.graphics.getWidth() / 2f;
    h = Core.graphics.getHeight() / 2f;
    modal = true;
  }
  
  @Override
  public void main(Table t) {
    t.add(
            new Button(
                "宇宙",
                () -> {
                  if (!UniverseFragment.showing) {
                    UniverseFragment.showing = true;
                   
                    // 同步宇宙相机到游戏相机位置
                    Render.universeCamera.position.set(Core.camera.position);
                    Render.universeCamera.width = Core.camera.width;
                    Render.universeCamera.height = Core.camera.height;

                    // 弹出宇宙菜单
                    if (UI.universe != null && UI.universe.root != null) {
                      UI.universe.root.remove();
                    }
                    UI.universe = new UniverseFragment();
                    UI.universe.build();

                    // 隐藏 HUD
                    UI.hud.hideHUD();
                    remove();
                  }
                }))
        .size(120f, 50f)
        .left()
        .top();
  }
  
  
}