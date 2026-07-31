package caliniya.armavoke.world.stars;

import arc.math.geom.QuadTree.QuadTreeObject;
import arc.math.geom.*;

// 表示宇宙中的一个星系节点
public class StarNode implements QuadTreeObject {

  public float x, y, size;
  
  public int id;
  public String name;

  @Override
  public void hitbox(Rect box) {
    box.set(x, y, size, size);
  }
}