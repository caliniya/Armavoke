package caliniya.armavoke.world.stars;

import arc.math.geom.QuadTree;
import arc.struct.IntQueue;
import arc.struct.ObjectMap;
import caliniya.armavoke.base.tool.ObjectSet;

// 现在这表示一片星域的实例
public class StarMap {

  public ObjectSet<StarRoad> roadSet = new ObjectSet<StarRoad>();
  public ObjectSet<StarNode> nodeSet = new ObjectSet<>();
  
  public QuadTree<StarRoad> tree;
  
  public StarMap(float w , float h){
    tree = new QuadTree();
  }
  
  //连接两点
  public void link(StarNode A, StarNode B) {
     roadSet.add(new StarRoad(A, B));
  }
  
  // 向图中添加一个节点
  public void addNode(StarNode node) {
    nodeSet.add(node);
  }
  
}