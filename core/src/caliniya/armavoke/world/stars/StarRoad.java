package caliniya.armavoke.world.stars;

import arc.math.geom.QuadTree.QuadTreeObject;
import arc.math.geom.*;
import caliniya.armavoke.world.stars.StarNode;

public class StarRoad implements QuadTreeObject {

  public StarNode A, B;

  @Override
  public void hitbox(Rect box) {}

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
    return (this.A == null ? 0 : this.A.hashCode())
        + (this.B == null ? 0 : this.B.hashCode()); // A+B 和 B+A 结果相同
  }
}
