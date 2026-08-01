package caliniya.armavoke.world.stars;

import arc.func.*;
import arc.graphics.Camera;
import arc.math.geom.*;
import arc.struct.*;
import caliniya.armavoke.base.tool.ObjectSet;

// 表示一片星域？
public class StarMap {

  public ObjectSet<StarRoad> roadSet;
  public ObjectSet<StarNode> nodeSet;

  public QuadTree<StarRoad> tree;
  
  public Rect tempR = new Rect();

  public StarMap(float w, float h) {
    roadSet = new ObjectSet<StarRoad>();
    nodeSet = new ObjectSet<>();
    tree = new QuadTree<>(new Rect(0, 0, w, h));
  }

  // 连接两点，路径在此过程中自动创建
  public void link(StarNode A, StarNode B) {
    A.add(B);
    B.add(A);
    StarRoad.with(
        A,
        B,
        road -> {
          roadSet.add(road);
          tree.insert(road);
        });
  }

  // 向图中添加一个节点
  public void addNode(StarNode node) {
    nodeSet.add(node);
  }

  public void get(float x, float y, float w, float h, Cons<StarRoad> out) {
    tree.intersect(x, y, w, h, out);
  }

  public void get(Camera cam, Cons<StarRoad> out) {
    cam.bounds(tempR);
    tree.intersect(tempR , out);
  }
}
