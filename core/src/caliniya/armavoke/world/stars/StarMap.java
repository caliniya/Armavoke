package caliniya.armavoke.world.stars;

import arc.struct.IntQueue;
import arc.struct.ObjectMap;
import caliniya.armavoke.base.tool.ObjectSet;

// 要不这就静态吧？
public class StarMap {

  public static ObjectSet<StarRoad> roadSet = new ObjectSet<StarRoad>();
  public static ObjectSet<StarNode> nodeSet = new ObjectSet<>();
  
  // 祖传的id系统()
  private static int lastEntityID = 0;
  private static final IntQueue freeIDs = new IntQueue();

  public static int assignID() {
    if (freeIDs.size > 0) {
      return freeIDs.removeFirst();
    }
    return ++lastEntityID;
  }

  public static int freeID(int id) {
    if (id > 0 && id <= lastEntityID) {
      freeIDs.addLast(id);
    }
    return -1;
  }

  public static int checkoutID(int id) {
    if (id <= 0) return 0;

    if (id > lastEntityID) {
      // 中间空缺的ID全部回收，供后续 assignID 重用
      for (int i = lastEntityID + 1; i < id; i++) {
        freeIDs.addLast(i);
      }
      lastEntityID = id;
    } else {
      // id 落在已分配区间：若它正躺在空闲队列里，取出以防被重复分配
      freeIDs.removeValue(id);
    }
    return id;
  }
  
  //连接两点
  public void link(StarNode A , StarNode B) {
     roadSet.add(new StarRoad(A,B));
  }
  
  // 向图中添加一个
  public void addNode(StarNode node){
    nodeSet.add(node);
  }
  
}
