package caliniya.armavoke.type.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Entities;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.type.Weapon;

public class WeaponType implements Cloneable {

  public String name;
  public TextureRegion region;

  // 基础属性
  public float range = 2000f;
  public float rotateSpeed = 5f;
  public float reload = 60f;
  public float x = 0f, y = 0f;
  public float shootX = 0f, shootY = 0f;

  // 镜像控制
  public boolean mirror = true;
  public boolean flipSprite = false;
  public boolean alternate = true;
  public int otherSide = -1;

  // 标记是否为生成的镜像副本
  public boolean isMirror = false;

  public BulletType bullet;

  public boolean rotate = true;
  public float shootCone = 2f;

  public WeaponType(String name) {
    this.name = name;
  }

  public void load(String UnitName) {
    String textureName = UnitName + "-" + this.name;
    region = Core.atlas.find(textureName, "air");
    bullet.load();
  }

  // --- 目标查找 ---

  /** 默认查找目标实现，使用武器射程进行索敌。 特殊武器（如导弹、激光）可覆写此方法实现自定义索敌逻辑。 */
  public void findTarget(Weapon w, float wx, float wy) {
    w.target = Entities.closestEnemy(w.owner.team, wx, wy, range);
  }

  // --- 镜像 ---

  public void flip() {
    this.x *= -1;
    this.shootX *= -1;
    this.flipSprite = !this.flipSprite;
    this.isMirror = true;
  }

  public WeaponType copy() {
    try {
      return (WeaponType) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
