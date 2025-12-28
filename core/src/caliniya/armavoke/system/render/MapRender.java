package caliniya.armavoke.system.render;

import arc.Core;
import arc.Events;
import arc.graphics.Camera;
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

  @Override
  public MapRender init() {
    Events.run(EventType.events.Mapinit , () -> rebuildAll());
    WorldData.initWorld();
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
