package caliniya.armavoke.game.data;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.World;
import arc.struct.IntQueue;
import java.util.PriorityQueue;

public class RouteData {

  private static final int MAX_PRECALC_RADIUS = 4;//预生成的最大单元集
  //1 1 2 2-3 3 4-5 4 5-6
  private static final int MAX_CAPABILITY = 2;//预计生成的最大跨越能力
  
  public static World world;
  public static int W, H;
  
  public static boolean[] baseMap;//基础障碍图
  
  public static IntMap IMap;//距离场图
  
  public static NavLayer[] navs = new NavLayer[MAX_PRECALC_RADIUS + MAX_CAPABILITY];
  
  public class NavLayer{
    
    public static boolean[] Map;//该层的障碍图
    
    public NavLayer(int size) {
      Map = new boolean[size];
    }
  }
  
  public static void init() {
    world = WorldData.world;//更新世界引用
    W = world.W;
    H = world.H;
    int index = W * H;
    for(int i = 0; i < index; ++i) {
    	if(world.isSolid(i)) {
    		baseMap[i] = true;
    	}
    }//初始化基本图
    
    
    
    for(int r = 2; r < MAX_PRECALC_RADIUS; ++r) {
    	
    }
  }
  
  
  
  private static class Node implements Comparable<Node> {
    int x, y;
    Node parent;
    float g, h;

    public Node(int x, int y, Node parent, float g, float h) {
      this.x = x;
      this.y = y;
      this.parent = parent;
      this.g = g;
      this.h = h;
    }

    @Override
    public int compareTo(Node o) {
      return Float.compare(g + h, o.g + o.h);
    }
  }
}
