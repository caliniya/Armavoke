package caliniya.armavoke.system.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.util.Align;
import arc.graphics.g2d.Lines;
import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;
import caliniya.armavoke.world.Block;
import caliniya.armavoke.ui.*;
import caliniya.armavoke.ui.fragment.UniverseFragment;

public class BlockRender extends System<BlockRender> {

  @Override
  public BlockRender init() {
    this.index = 6; // 渲染层级，确保在地图之上、单位之下（根据你的 System 排序逻辑）
    return super.init(false);
  }

  @Override
  public void update() {
    if (UniverseFragment.showing) return;
    // 遍历所有建筑
    for (Building b : WorldData.buildings) {
      if (b == null || b.block == null) continue;

      // 剔除检测：如果不在视野内则跳过
      if (shouldDraw(b.x, b.y, b.block.psize)) {
        // 绘制建筑 (调用 Building 内部的 draw 逻辑，会处理旋转)
        b.draw();

        // 调试绘制
        if (UnitRender.debug) { // 复用 UnitRender 的 debug 开关
          drawDebug(b, b.x, b.y, b.block.psize);
        }
      }
    }
  }

  // 视野剔除判断
  private boolean shouldDraw(float x, float y, float size) {
    // 使用相机位置进行判断
    float viewX = Core.camera.position.x;
    float viewY = Core.camera.position.y;
    // 稍微扩大缓冲区，防止边缘的建筑突然消失
    float buffer = size;
    float w = Core.camera.width / 2f + buffer;
    float h = Core.camera.height / 2f + buffer;
    return x > viewX - w && x < viewX + w && y > viewY - h && y < viewY + h;
  }

  // 调试绘制
  private void drawDebug(Building b, float pixelX, float pixelY, float drawSize) {
    // 1. 绘制包围盒 (绿色)

    Draw.color(Color.green);
    Lines.stroke(4f);
    // 绘制基于 size 的包围盒
    Lines.rect(pixelX, pixelY, drawSize, drawSize);

    // 3. 绘制占据的实际格子 (黄色细线)
    // 对于异形建筑，这比包围盒更准确
    if (b.shapeOffsets != null) {
      Draw.color(Color.cyan);
      Lines.stroke(1f);
      for (int i = 0; i < b.shapeOffsets.length; i += 2) {
        float tx = (b.tx + b.shapeOffsets[i]) * WorldData.TILE_SIZE;
        float ty = (b.ty + b.shapeOffsets[i + 1]) * WorldData.TILE_SIZE;
        Lines.rect(tx, ty, WorldData.TILE_SIZE, WorldData.TILE_SIZE);
      }
    }

    // 4. 绘制旋转角度 (青色文字)
    Fonts.def.draw("R:" + b.angle, pixelX + drawSize / 2f, pixelY + drawSize + 10f, Align.center);

    Draw.color(); // 重置颜色
  }
}
