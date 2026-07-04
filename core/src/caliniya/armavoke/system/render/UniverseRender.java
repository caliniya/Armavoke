package caliniya.armavoke.system.render;

import arc.Core;
import arc.graphics.Camera;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import caliniya.armavoke.base.shaders.SpaceShader;
import caliniya.armavoke.system.System;
import caliniya.armavoke.ui.fragment.UniverseFragment;

/**
 * 渲染(网格)宇宙<br>
 * 调用祖传的太空着色器铺设星空背景，<br>
 * 再叠加白色网格线——网格大小 128 像素，粗细 5 像素。<br>
 * 仅在宇宙视图({@link UniverseFragment#showing})激活时渲染。
 */
public class UniverseRender extends System<UniverseRender> {

    /** 网格大小（像素） */
    public static final float GRID_SIZE = 128f;

    /** 网格线粗细 */
    public static final float GRID_THICKNESS = 5f;

    /** 网格颜色 */
    public static final Color GRID_COLOR = Color.white;

    /** 太空着色器 */
    private SpaceShader spaceShader;

    @Override
    public UniverseRender init() {
        this.index = 2; // 在 MapRender(5) 之前，先铺宇宙背景
        spaceShader = new SpaceShader();
        return super.init(false);
    }

    @Override
    public void update() {
        if (!UniverseFragment.showing) return;

        // 1. 太空背景
        spaceShader.render();

        // 2. 网格线
        Camera cam = Core.camera;

        float viewLeft = cam.position.x - cam.width / 2f;
        float viewBottom = cam.position.y - cam.height / 2f;
        float viewRight = cam.position.x + cam.width / 2f;
        float viewTop = cam.position.y + cam.height / 2f;

        // 对齐到网格
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
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
