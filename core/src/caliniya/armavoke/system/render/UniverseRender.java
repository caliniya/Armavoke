package caliniya.armavoke.system.render;

import arc.Core;
import arc.graphics.Camera;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import caliniya.armavoke.base.shaders.SpaceShader;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.system.System;
import caliniya.armavoke.system.input.UniverseCameraInput;
import caliniya.armavoke.ui.fragment.UniverseFragment;

/**
 * 渲染(网格)宇宙<br>
 * 太空着色器背景 + 白色网格线。<br>
 * 仅在宇宙视图激活时渲染，使用无边界限制的 universeCamera。
 */
public class UniverseRender extends System<UniverseRender> {

    /** 网格大小（像素） */
    public static final float GRID_SIZE = 128f;

    /** 网格线粗细 */
    public static final float GRID_THICKNESS = 5f;

    /** 网格颜色 */
    public static final Color GRID_COLOR = Color.white;

    /** 太空背景着色器 */
    private SpaceShader background;

    @Override
    public UniverseRender init() {
        this.index = 2;
        background = new SpaceShader();
        background.parallaxScale = 0.05f;
        background.baseScale = 0.6f;
        return super.init(false);
    }

    @Override
    public void update() {
        if (!UniverseFragment.showing) return;

        Camera cam = Render.universeCamera;
        float zoom = UniverseCameraInput.zoom;

        // 切换到宇宙相机投影
        Draw.proj(cam);

        // 1. 太空背景
        background.render(cam, zoom);

        // 2. 网格
        float viewLeft = cam.position.x - cam.width / 2f;
        float viewBottom = cam.position.y - cam.height / 2f;
        float viewRight = cam.position.x + cam.width / 2f;
        float viewTop = cam.position.y + cam.height / 2f;

        float sx = (float) Math.floor(viewLeft / GRID_SIZE) * GRID_SIZE;
        float sy = (float) Math.floor(viewBottom / GRID_SIZE) * GRID_SIZE;
        float ex = (float) Math.ceil(viewRight / GRID_SIZE) * GRID_SIZE;
        float ey = (float) Math.ceil(viewTop / GRID_SIZE) * GRID_SIZE;

        Draw.color(GRID_COLOR);
        Lines.stroke(GRID_THICKNESS);

        for (float x = sx; x <= ex; x += GRID_SIZE) {
            Lines.line(x, sy, x, ey);
        }
        for (float y = sy; y <= ey; y += GRID_SIZE) {
            Lines.line(sx, y, ex, y);
        }

        Draw.color();

        // 恢复游戏相机投影
        Draw.proj(Core.camera);
    }

    @Override
    public void dispose() {
        if (background != null) background.dispose();
        super.dispose();
    }
}
