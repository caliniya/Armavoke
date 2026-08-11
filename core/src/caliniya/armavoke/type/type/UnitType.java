package caliniya.armavoke.type.type;

import arc.*;

import arc.util.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.graphics.g2d.*;
import caliniya.armavoke.ui.*;
import caliniya.armavoke.game.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.base.api.*;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.base.game.*;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.type.ability.ForceFieldAbility;
import caliniya.armavoke.type.ability.ShieldAbility;

public class UnitType extends ContentType implements DrawType<Unit>, TechNodeContent {

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

  // 单位的物品容量，使用通用的物品模块规则
  public int itemCap = 50;

  public Ar<WeaponType> weapons = new Ar<WeaponType>();

  // 渲染资源
  public TextureRegion region, cell;

  public UnitType(String name) {
    super(name, CType.Unit);
  }

  @Override
  public TechNodeContent[] requirements() {
    return requirements; // ContentType 里的前置字段（默认 null）
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
  public Unit create() {
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
      weapon.type.draw(weapon);
    }
    Draw.color();
  }

  /**
   * 绘制单位血条（默认样式）：单位右边缘垂直条， **固定分段 + 段内独立填充**。
   *
   * <p>1. 固定分段：总容量 = 核心Max + 护甲Max + 护盾原始Max， 各段宽 = 条长 × 该层Max/总容量（段位置固定，互不影响）；
   *
   * <p>2. 段内独立渲染：每段从左到右按「当前 / 该层最大」填充， 左侧固定、右侧随当前值缩；
   *
   * <p>3. 护盾段比例用等效容量：比例 = 当前/最大（接近线性）。 子类可覆写此方法定制血条。
   */
  public void drawHealthBar(Unit u) {
    ShieldAbility shield = u.shield();

    float coreMax = Math.max(0f, u.maxHealth);
    float core = Math.max(0f, u.health);
    float armorMax = Math.max(0f, u.armorMax);
    float armor = Math.max(0f, u.armor);
    // 护盾段 = 单体护盾 + 力场护盾 容量之和
    float shieldMax = 0f, shieldCur = 0f;
    if (shield != null) {
      shieldMax += Math.max(0f, shield.max);
      shieldCur += Math.max(0f, shield.current);
    }
    ForceFieldAbility forceField = u.forceField();
    if (forceField != null) {
      shieldMax += Math.max(0f, forceField.capacityMax());
      shieldCur += Math.max(0f, forceField.capacity());
    }

    float totalMax = coreMax + armorMax + shieldMax;
    if (totalMax <= 0f) return;

    float barLen = u.size * 1.5f;
    float barW = 6f;
    // 以单位中心为原点：血条贴右边缘（半径 0.5×size），垂直居中于单位中心
    float startX = u.x + u.size * 0.5f;
    float startY = u.y - u.size * 0.75f; // 下端 -0.75×size，上端 +0.75×size（对称）

    // 底色
    Draw.color(Color.darkGray);
    Fill.rect(startX + barW / 2f, startY + barLen / 2f, barW, barLen);

    // 固定分段
    float coreSeg = barLen * coreMax / totalMax;
    float armorSeg = barLen * armorMax / totalMax;
    float shieldSeg = barLen * shieldMax / totalMax;

    // 核心段（最下，红）
    if (coreSeg > 0f && core > 0f) {
      float h = coreSeg * (core / coreMax);
      Draw.color(Color.scarlet);
      Fill.rect(startX + barW / 2f, startY + h / 2f, barW, h);
    }

    // 护甲段（中，白）
    if (armorSeg > 0f && armor > 0f) {
      float h = armorSeg * (armor / armorMax);
      Draw.color(Color.lightGray);
      Fill.rect(startX + barW / 2f, startY + coreSeg + h / 2f, barW, h);
    }

    // 护盾段（最上，蓝）：等效容量比例（= 当前/最大，线性）
    if (shieldSeg > 0f && shieldCur > 0f) {
      float h = shieldSeg * (shieldCur / shieldMax);
      Draw.color(Color.sky);
      Fill.rect(startX + barW / 2f, startY + coreSeg + armorSeg + h / 2f, barW, h);
    }

    Draw.color();
  }

  public void drawDebug(Unit u) {

    Draw.color(Color.yellow);
    Lines.stroke(2f);

    if (u.hitboxData != null) {
      for (int i = 0; i < u.hitboxData.length; i += 3) {
        float cx = u.hitboxData[i];
        float cy = u.hitboxData[i + 1];
        float s = u.hitboxData[i + 2];
        Lines.rect(cx - s / 2f, cy - s / 2f, s, s);
      }
    }

    // 绘制计算出的外接圆 (新增，用于验证 size 计算是否正确)
    Draw.color(Color.sky);
    float radius = u.size / 2f;
    Lines.circle(u.x, u.y, radius);
    Draw.color(Color.yellow); // 还原颜色

    if (Math.abs(u.speedX) > 0.001f || Math.abs(u.speedY) > 0.001f) {
      Draw.color(Color.magenta);
      float scale = 20f;
      Lines.line(u.x, u.y, u.x + u.speedX * scale, u.y + u.speedY * scale);
      Fonts.def.draw(Strings.format(u.speedX + " " + u.speedY), u.x, u.y + size + 8f, Align.center);
    }

    if (u.targetX != 0 || u.targetY != 0) {
      Draw.color(Color.orange);
      Lines.line(u.x, u.y, u.targetX, u.targetY);
      float s = 8f;
      Lines.line(u.targetX - s, u.targetY - s, u.targetX + s, u.targetY + s);
      Lines.line(u.targetX - s, u.targetY + s, u.targetX + s, u.targetY - s);
    }
    if (u.path != null && !u.path.isEmpty()) {
      Draw.color(Color.cyan);

      float lastX = u.x;
      float lastY = u.y;

      for (int i = u.pathIndex; i < u.path.size; i++) {
        Point2 p = u.path.get(i);
        float wx = p.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
        float wy = p.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;

        Lines.line(lastX, lastY, wx, wy);
        Fill.square(wx, wy, 3f);
        lastX = wx;
        lastY = wy;
      }
    }

    for (Weapon weapon : u.weapons) {
      weapon.type.drawDebug(weapon);
    }
    Draw.color();
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
        weapon.otherSide = weapons.size;
        copy.otherSide = weapons.size - 1;

        weapons.add(copy);
      }
    }
  }
}
