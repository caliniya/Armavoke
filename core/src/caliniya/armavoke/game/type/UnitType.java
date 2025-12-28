package caliniya.armavoke.game.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;

public class UnitType extends ContentType {

  public float speed = 6f,//格每秒
   health = 100f,
   speedt,//像素每帧
   rotationSpeend = 50f//旋转速度(单位帧每度？)
   ;
  public float w = 100f, h = 180f;

  public Ar<WeaponType> weapons = new Ar<WeaponType>().add(new WeaponType("aa"));

  // 渲染资源
  public TextureRegion region, cell;

  public UnitType(String name) {
    super(name, CType.Unit);
  }

  // 加载资源 (在 Assets 加载完成后调用)
  public void load() {
    this.speedt = (speed * WorldData.TILE_SIZE) / 60f;
    region = Core.atlas.find(name, "white");
    cell = Core.atlas.find(name + "-cell", "white");
    for (WeaponType weapon : weapons) {
      weapon.load(this);
    }
  }

  // 工厂方法：创建一个该类型的单位
  public Unit create() {
    return Unit.create(this);
  }

  // 带坐标
  public Unit create(float x, float y) {
    return Unit.create(this, x, y);
  }

  public void addWeapon(WeaponType weapon) {
    weapons.add(weapon);
  }
}
