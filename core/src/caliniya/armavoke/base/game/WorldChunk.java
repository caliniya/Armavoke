package caliniya.armavoke.world;

public class WorldChunk {
  // 必须是 2 的 N 次幂，方便位运算优化
  public static final int SIZE = 32; 
  public static final int AREA = SIZE * SIZE;
  
  // 位运算掩码：x % 32 等价于 x & 31
  public static final int MASK = SIZE - 1; 
  // 位移量：x / 32 等价于 x >> 5,  x * 32 等价于 x << 5
  public static final int SHIFT = 5; 

  public short[] floorIds;
  public short[] envblockIds;
  
  public boolean empty = true;

  public WorldChunk() {
    floorIds = new short[AREA];
    envblockIds = new short[AREA];
  }
  
  //注意，所有的获取方法没有做检查，调用时请确保不会越界
  public short getFloor(int localX, int localY) {
    return floorIds[(localY << SHIFT) | localX];
  }

  public short getENVBlock(int localX, int localY) {
    return envblockIds[(localY << SHIFT) | localX];
  }

  public void setFloor(int localX, int localY, short id) {
    floorIds[(localY << SHIFT) | localX] = id;
    empty = false;
  }

  public void setENVBlock(int localX, int localY, short id) {
    envblockIds[(localY << SHIFT) | localX] = id;
    empty = false;
  }
}