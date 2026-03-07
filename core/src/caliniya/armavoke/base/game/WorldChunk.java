package caliniya.armavoke.world;

import caliniya.armavoke.game.Building;

public class WorldChunk {
  // 必须是 2 的 N 次幂
  public static final int SIZE = 32; 
  public static final int AREA = SIZE * SIZE;
  public static final int MASK = SIZE - 1; 
  public static final int SHIFT = 5; 

  // 数据层 1: 地板
  public short[] floorIds;
  // 数据层 2: 环境方块 (墙壁等)
  public short[] envblockIds;
  // 数据层 3: 建筑实例 (新增)
  public Building[] buildings;
  
  public boolean empty = true;

  public WorldChunk() {
    floorIds = new short[AREA];
    envblockIds = new short[AREA];
    buildings = new Building[AREA]; // 初始化引用数组
  }
  
  // --- Floor ---
  public short getFloor(int localX, int localY) {
    return floorIds[(localY << SHIFT) | localX];
  }
  public void setFloor(int localX, int localY, short id) {
    floorIds[(localY << SHIFT) | localX] = id;
    empty = false;
  }

  // --- ENVBlock ---
  public short getENVBlock(int localX, int localY) {
    return envblockIds[(localY << SHIFT) | localX];
  }
  public void setENVBlock(int localX, int localY, short id) {
    envblockIds[(localY << SHIFT) | localX] = id;
    empty = false;
  }

  // --- Building (新增) ---
  public Building getBuilding(int localX, int localY) {
    return buildings[(localY << SHIFT) | localX];
  }
  public void setBuilding(int localX, int localY, Building build) {
    buildings[(localY << SHIFT) | localX] = build;
    empty = false;
  }
}