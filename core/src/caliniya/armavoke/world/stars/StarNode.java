package caliniya.armavoke.world.stars;

import arc.math.geom.QuadTree.*;
import arc.math.geom.*;
import caliniya.armavoke.base.tool.*;

// 表示宇宙中的一个星系节点
public class StarNode implements QuadTreeObject {
  
  // 我不认为一个节点会比64像素还要大
  public float x, y, size;

  public int id;
  public String name;

  public Ar<StarNode> nei;

  public StarNode(float x, float y, String name) {
    this.x = x;
    this.y = y;
    this.name = name;
    
    //依旧祖传的id系统
    this.id = StarMap.assignID();
  }
  
  public void add(StarNode... neig){
    nei.add(neig);
  }

  @Override
  public void hitbox(Rect box) {
    box.set(x, y, size, size);
  }
}
