package caliniya.armavoke.type.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;

public class WeaponType implements Cloneable {

  public String name;
  public TextureRegion region;

  // 基础属性
  public float range = 1000f;
  public float rotateSpeed = 5f;
  public float reload = 60f;
  public float x = 0f, y = 0f;
  public float shootX = 0f, shootY = 0f; // 枪口偏移

  // 镜像控制
  public boolean mirror = true;
  public boolean flipSprite = false;
  public boolean alternate = true;
  public int otherSide = -1;

  // 标记是否为生成的镜像副本
  public boolean isMirror = false;

  public BulletType bullet;
  
  public boolean rotate = true; // 默认是可旋转炮塔，false 为固定武器
  public float shootCone = 2f; // 固定武器允许开火的锥形角度 (只有单位对准了这个角度内才能开火)

  public WeaponType(String name) {
    this.name = name;
  }

  public void load(String parentUnitName) {
    String textureName = parentUnitName + "-" + this.name;
    region = Core.atlas.find(textureName, "air");
    bullet.load();
  }

  public void flip() {
    this.x *= -1;
    this.shootX *= -1; // 枪口X也要反转
    this.flipSprite = !this.flipSprite;
    this.isMirror = true; // 标记为镜像
  }

  public WeaponType copy() {
    try {
      return (WeaponType) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
