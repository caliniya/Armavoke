package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.core.UI;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.ui.Styles;

/**
 * 宇宙界面菜单 — 切换到宇宙视图时显示<br>
 * 提供网格开关等宇宙相关控制。
 */
public class UniverseFragment {

    /** true=宇宙视图, false=地图视图(默认) */
    public static boolean showing = false;

    public Table root;

    public void build() {
        root = new Table();
        root.setFillParent(true);
        root.top().left();
        //root.background(Styles.background);

        root.table(menu -> {
            menu.defaults().pad(8f).width(200f);

            menu.add("[white]宇宙视图[]").center().padBottom(12f).row();
            menu.add().height(4f).row();

            // 返回地图
            menu.add(new Button("返回地图", () -> {
                showing = false;
                root.remove();
                // 恢复 HUD
                if (UI.hud != null) {
                    UI.hud.showHUD();
                }
            })).height(48f);
            menu.row();

            // 网格开关（预留）
            menu.add(new Button("网格: 开", () -> {
                // TODO: 切换 UniverseRender 网格显示
            })).height(48f);
            menu.row();
        }).pad(20f);

        Core.scene.root.addChild(root);
    }
}
