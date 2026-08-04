package caliniya.armavoke.world.stars;

import arc.Core;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.QuadTree.*;
import arc.math.geom.*;
import arc.util.io.Writes;
import caliniya.armavoke.base.tool.*;

// 表示宇宙中的一个星系节点
public class StarNode implements QuadTreeObject {

  // 我不认为一个节点会比64像素还要大
  // 反正现在绘制由路径取决
  public float x, y, size;

  /** 节点唯一ID（星图内），存档/读档的稳定标识 */
  public int id;

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
    this.size = 64f;
  }

  public void add(StarNode... neig) {
    nei.add(neig);
  }

  /** 与指定节点的距离平方（比距离更常用，避免开方） */
  public float dst2(StarNode other) {
    return Mathf.dst2(x, y, other.x, other.y);
  }

  /** 与指定节点的距离 */
  public float dst(StarNode other) {
    return Mathf.dst(x, y, other.x, other.y);
  }

  public void draw() {
    Draw.rect(main, x, y, size, size);
  }

  /** 序列化自身（含邻接引用；邻居由 StarMap 读取后统一重建） */
  public void write(Writes w) {
    w.i(id);
    w.f(x);
    w.f(y);
    w.f(size);
    w.str(name);
    w.s(nei.size);
    for (StarNode n : nei) {
      w.i(n.id);
    }
  }

  @Override
  public void hitbox(Rect out) {
    float half = size / 2f;
    out.set(x - half, y - half, size, size);
  }
}
