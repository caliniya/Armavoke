package caliniya.armavoke.game.type;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;

public class UnitType extends ContentType {

  public float speed = 60f, // 格每秒
      health = 100f,
      speedt, // 像素每帧
      rotationSpeend = 1f // 旋转速度(单位帧每度？)
  ;

  // 物理数据，若碰撞盒为空 则使用size进行填充
  public float[] hitbox = null;
  public float size = 100f;

  // 单位的探测距离，位于此范围内的敌方会被标记出来(todo)，没有被标记的敌人仍然可以被攻击
  public float scanDistance = 200f;
  
  public int itemCap = 50;

  public Ar<WeaponType> weapons = new Ar<WeaponType>();

  // 渲染资源
  public TextureRegion region, cell;

  public UnitType(String name) {
    super(name, CType.Unit);
  }

  // 加载资源 (在 Assets 加载完成后调用)
  public void load() {
    this.speedt = (speed * WorldData.TILE_SIZE) / 60f;
    region = Core.atlas.find(name, "white");
    cell = Core.atlas.find(name + "-cell", "air");
    for (WeaponType weapon : weapons) {
      weapon.load(name);
    }
  }


  public Unit create(TeamTypes team, float x, float y) {
    return Unit.create(team, this, x, y);
  }
  
  // 用于存档读取的创建
  public Unit create(){
    return Unit.create(this);
  }

  public void draw(Unit u) {
    // 默认的绘制逻辑
    if (u.isSelected) {
      Draw.color(Color.green);
      Lines.stroke(2f);
      Lines.circle(u.x, u.y, u.size + 4);
      Draw.color();
    }

    Draw.rect(u.region, u.x, u.y, u.rotation);
    Draw.rect(u.cell, u.x, u.y, u.rotation);

    // 绘制武器
    for (Weapon weapon : u.weapons) {
      float wRot = u.rotation + weapon.rotation;
      Draw.rect(weapon.type.region, weapon.wx, weapon.wy, wRot);
    }
  }

  public void update(Unit u, float dt) {
    // TODO: 以后再说
  }

  public void addWeapons(WeaponType... newWeapons) {
    for (WeaponType weapon : newWeapons) {
      // 添加主武器
      weapon.isMirror = false;
      weapons.add(weapon);

      // 处理镜像
      if (weapon.mirror) {
        WeaponType copy = weapon.copy();
        copy.flip();
        // 建立索引关联
        // 主武器索引 = size-1, 镜像索引 = size
        weapon.otherSide = weapons.size;
        copy.otherSide = weapons.size - 1;

        weapons.add(copy);
      }
    }
  }
}
