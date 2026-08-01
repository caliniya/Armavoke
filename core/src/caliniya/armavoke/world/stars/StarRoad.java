package caliniya.armavoke.world.stars;

import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.QuadTreeObject;
import caliniya.armavoke.world.stars.StarNode;

public class StarRoad implements QuadTreeObject{

  public StarNode A, B;

  public StarRoad(StarNode A, StarNode B) {
    this.A = A;
    this.B = B;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof StarRoad) {
      StarRoad s = (StarRoad) o;
      return (this.A == s.A && this.B == s.B) || (this.B == s.A && this.A == s.B);
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return (this.A == null ? 0 : this.A.hashCode()) + (this.B == null ? 0 : this.B.hashCode());
  }
  
  @Override
  public void hitbox(Rect box) {
    box.set(Math.min(A.x,B.x),Math.min(A.y,B.y),Math.abs(A.x -B.x),Math.abs(A.y-B.y));
  }
}
