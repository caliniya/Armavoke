package caliniya.armavoke.world.stars;

import arc.Core;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.QuadTree.*;
import arc.math.geom.*;
import caliniya.armavoke.base.tool.*;

// 表示宇宙中的一个星系节点
public class StarNode {

  // 我不认为一个节点会比64像素还要大
  // 反正现在绘制由路径取决
  public float x, y, size;

  public String name;

  public static TextureRegion main;

  public Ar<StarNode> nei;

  {
    main = Core.atlas.find("starNode");
  }

  public StarNode(float x, float y, String name) {
    this.x = x;
    this.y = y;
    this.name = name;
    this.nei = new Ar<>();
  }

  public void add(StarNode... neig) {
    nei.add(neig);
  }

  public void draw() {
    Draw.rect(main, x, y, 64, 64);
  }
}
