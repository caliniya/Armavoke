package caliniya.armavoke.system.render;

import arc.*;
import arc.util.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import caliniya.armavoke.ui.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.world.*;
import caliniya.armavoke.system.*;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.ui.fragment.*;

public class BlockRender extends caliniya.armavoke.system.System<BlockRender> {

  @Override
  public BlockRender init() {
    this.index = 6;
    return super.init(false);
  }

  @Override
  public void update() {
    if (UniverseFragment.showing) return;
    // 遍历所有建筑
    for (Building b : WorldData.buildings) {
      if (b == null || b.block == null || b.health <= 0f) continue;

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
    Lines.rect(pixelX - b.block.psize / 2, pixelY - b.block.psize / 2, drawSize, drawSize);

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
    Fonts.def.draw(b.x + "   "+b.y, pixelX + drawSize / 2f, pixelY + drawSize + 10f, Align.center);
    Fonts.def.draw(
        Strings.format("" + b.health),
        b.x - b.block.size,
        b.y - b.block.size + b.block.size + 8f,
        Align.center);
    Draw.color(); // 重置颜色
  }
}
