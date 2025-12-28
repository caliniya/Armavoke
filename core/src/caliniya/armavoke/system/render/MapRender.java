package caliniya.armavoke.system.render;

import arc.Core;
import arc.Events;
import arc.graphics.Camera;
import arc.graphics.g2d.Font;
import caliniya.armavoke.ui.Fonts;
import caliniya.armavoke.game.data.RouteData;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.Color;
import arc.util.Align;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mathf;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.BasicSystem;
import caliniya.armavoke.world.World;
import caliniya.armavoke.base.game.*;

public class MapRender extends BasicSystem<MapRender> {
  public static final float TILE_SIZE = 32f;
  public static World world;
  public Camera camera = Core.camera;

  // 存储所有区块的二维数组
  private MapChunk[][] chunks;
  private int chunksW, chunksH;

  public boolean debug = true;
  private final GlyphLayout layout = new GlyphLayout();

  @Override
  public MapRender init() {
    Events.run(EventType.events.Mapinit, () -> rebuildAll());
    //WorldData.initWorld();
    world = WorldData.world;
    this.index = 8;
    initChunks();

    return super.init();
  }

  private void initChunks() {
    // 计算横向和纵向有多少个区块
    chunksW = Mathf.ceil((float) world.W / MapChunk.SIZE);
    chunksH = Mathf.ceil((float) world.H / MapChunk.SIZE);

    chunks = new MapChunk[chunksW][chunksH];

    for (int x = 0; x < chunksW; x++) {
      for (int y = 0; y < chunksH; y++) {
        chunks[x][y] = new MapChunk(x, y);
      }
    }
  }

  /** 重新开始渲染 */
  public void rebuildAll() {
    if (chunks != null) {
      for (int x = 0; x < chunksW; x++) {
        for (int y = 0; y < chunksH; y++) {
          if (chunks[x][y] != null) {
            chunks[x][y].dispose();
          }
        }
      }
    }

    // 2. 更新世界引用 (防止指向旧的 World 对象)
    world = WorldData.world;

    // 3. 重新初始化区块数组结构
    initChunks();
  }

  /** 仅重新渲染 适用于地图尺寸没变，但想强制重绘所有画面的情况 */
  public void flagAllDirty() {
    if (chunks == null) return;

    for (int x = 0; x < chunksW; x++) {
      for (int y = 0; y < chunksH; y++) {
        if (chunks[x][y] != null) {
          chunks[x][y].dirty = true;
        }
      }
    }
  }

  @Override
  public void update() {
    if (!inited || chunks == null) return;

    // 计算摄像机视野范围内的 区块索引
    float viewLeft = camera.position.x - camera.width / 2f;
    float viewBottom = camera.position.y - camera.height / 2f;
    float viewRight = camera.position.x + camera.width / 2f;
    float viewTop = camera.position.y + camera.height / 2f;

    // 将像素坐标转换为区块索引
    int startX = (int) (viewLeft / MapChunk.PIXEL_SIZE);
    int startY = (int) (viewBottom / MapChunk.PIXEL_SIZE);
    int endX = (int) (viewRight / MapChunk.PIXEL_SIZE);
    int endY = (int) (viewTop / MapChunk.PIXEL_SIZE);

    // 限制在数组范围内
    startX = Mathf.clamp(startX, 0, chunksW - 1);
    startY = Mathf.clamp(startY, 0, chunksH - 1);
    endX = Mathf.clamp(endX, 0, chunksW - 1);
    endY = Mathf.clamp(endY, 0, chunksH - 1);

    // 只渲染视野内的区块
    for (int y = startY; y <= endY; y++) {
      for (int x = startX; x <= endX; x++) {
        if (chunks[x][y] != null) {
          chunks[x][y].render();
        }
      }
    }
    drawDebugInfo(viewLeft, viewBottom, viewRight, viewTop);
  }

  /** 绘制调试层：红色障碍块 + 距离场数值 */
  private void drawDebugInfo(float viewLeft, float viewBottom, float viewRight, float viewTop) {
    int startX = Mathf.clamp((int) (viewLeft / TILE_SIZE), 0, world.W - 1);
    int startY = Mathf.clamp((int) (viewBottom / TILE_SIZE), 0, world.H - 1);
    int endX = Mathf.clamp((int) (viewRight / TILE_SIZE), 0, world.W - 1);
    int endY = Mathf.clamp((int) (viewTop / TILE_SIZE), 0, world.H - 1);

    Font font = Fonts.def;
    float fontScale = 0.25f;
    font.getData().setScale(fontScale);

    // 获取调试数据
    // 请确保你在 RouteData 实现了 getDebugSolidMap (或者把变量设为 public)
    // 假设 RouteData.layers 是 public 的
    boolean[] solidMap = RouteData.layers[0].solidMap;
    
    // 如果还没初始化，就不画
    if (solidMap == null) return;

    for (int y = startY; y <= endY; y++) {
      for (int x = startX; x <= endX; x++) {
        
        // 【关键】使用 World 的标准方法计算索引
        int index = WorldData.world.coordToIndex(x, y);
        
        float drawX = x * TILE_SIZE + TILE_SIZE / 2f;
        float drawY = y * TILE_SIZE + TILE_SIZE / 2f;

        // ----------------------------------------------------
        // 诊断 1: 绘制障碍物 (红色背景)
        // ----------------------------------------------------
        if (index >= 0 && index < solidMap.length && solidMap[index]) {
          Draw.color(1f, 0f, 0f, 0.5f); // 半透明红
          Fill.rect(drawX, drawY, TILE_SIZE, TILE_SIZE);
        }

        // ----------------------------------------------------
        // 诊断 2: 绘制坐标文本 (白色)
        // 格式: "x,y"
        // ----------------------------------------------------
        Draw.color(Color.white);
        font.draw(x + "," + y, drawX, drawY + 8f, Align.center);

        // ----------------------------------------------------
        // 诊断 3: 绘制内存索引 (黄色)
        // 格式: "[index]"
        // ----------------------------------------------------
        Draw.color(Color.yellow);
        font.draw("[" + index + "]", drawX, drawY - 4f, Align.center);
      }
    }

    Draw.color();
    font.getData().setScale(1f);
  }
  
  public void flagUpdate(int worldGridX, int worldGridY) {
    int cx = worldGridX / MapChunk.SIZE;
    int cy = worldGridY / MapChunk.SIZE;

    if (cx >= 0 && cx < chunksW && cy >= 0 && cy < chunksH) {
      if (chunks[cx][cy] != null) {
        chunks[cx][cy].dirty = true;
      }
    }
  }

  @Override
  public void dispose() {
    if (chunks != null) {
      for (int x = 0; x < chunksW; x++) {
        for (int y = 0; y < chunksH; y++) {
          if (chunks[x][y] != null) chunks[x][y].dispose();
        }
      }
    }
    super.dispose();
  }
}
