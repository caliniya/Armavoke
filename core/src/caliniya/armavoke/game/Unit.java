package caliniya.armavoke.game;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.util.Align;
import arc.util.Strings;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.game.type.UnitType;
import caliniya.armavoke.core.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.module.ItemModule;
import caliniya.armavoke.type.type.*;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.ui.Fonts;

public class Unit extends Entity {

  public UnitType type;
  public TeamData teamData;

  public Ar<Weapon> weapons = new Ar<>();
  public Weapon mainFixedWeapon = null;

  // --- 物理属性 ---
  public float speedX, speedY, angle;
  public float rotationSpeed;
  public float rotation;
  public float angleToTarget, distToTarget;

  public boolean canShoot = true;

  // --- 导航属性 ---
  public float targetX, targetY;
  public Ar<Point2> path;
  public int pathIndex = 0;
  public boolean pathed;
  public boolean velocityDirty = true;

  // --- 状态属性 ---
  public boolean isSelected = false;
  public boolean moving = false;
  public float size, speed;
  public TextureRegion region, cell;
  public float pathFindCooldown = 0f;

  // --- 碰撞体积缓存 (世界坐标) ---
  /** 存储旋转后的碰撞盒数据。 格式: [世界X1, 世界Y1, 尺寸1, 世界X2, 世界Y2, 尺寸2, ...] */
  public float[] hitboxData;

  protected Unit() {}

  public static Unit create(TeamTypes team, UnitType type, float x, float y) {
    Unit u = Pools.obtain(Unit.class, Unit::new);
    u.type = type;
    u.team = team;
    u.teamData = team.data();
    u.teamData.updateChunk(u, -1, WorldData.getChunkIndex(x, y));
    Teams.add(u);
    u.x = x;
    u.y = y;
    u.item = new ItemModule(type.itemCap);
    u.init();
    this.id = Entities.assignID();
    WorldData.units.add(u);
    u.updateChunkPosition();
    u.updateTeamData();
    u.updateHitbox();
    return u;
  }

  public static Unit create(UnitType type) {
    Unit u = Pools.obtain(Unit.class, Unit::new);
    u.type = type;
    u.item = new ItemModule(type.itemCap);
    u.init();
    return u;
  }

  public void init() {
    if (this.type == null) {
      this.type = UnitTypes.test;
      Log.err(this.toString() + "@ No unitTpye used test");
    }

    this.speed = this.type.speedt;
    this.rotationSpeed = this.type.rotationSpeend;
    this.region = this.type.region;
    this.cell = this.type.cell;

    this.maxHealth = this.type.health;
    this.health = this.type.health;

    // --- 初始化碰撞数据数组 ---
    if (type.hitbox != null) {
      hitboxData = new float[type.hitbox.length];
      // 自动计算外接圆直径
      calculateBoundingSize();
    } else {
      hitboxData = new float[3];
      // 没有自定义体积时，使用类型定义的默认尺寸
      this.size = this.type.size;
    }

    canShoot = true; // 单位一般情况都是可以射击的

    weapons.clear();
    mainFixedWeapon = null;
    for (WeaponType wType : type.weapons) {
      Weapon w = new Weapon(wType, this);
      weapons.add(w);
      if (!wType.rotate && mainFixedWeapon == null) {
        mainFixedWeapon = w;
      }
    }

    this.rotation = 0f;
    this.speedX = 0f;
    this.speedY = 0f;

    this.targetX = this.x;
    this.targetY = this.y;
  }

  /** 根据自定义碰撞体积计算最小外接圆直径 并赋值给 size */
  private void calculateBoundingSize() {
    if (type.hitbox == null) return;

    float maxRadiusSq = 0f;

    // 遍历所有碰撞方块，找到离原点最远的角点距离
    for (int i = 0; i < type.hitbox.length; i += 3) {
      float cx = type.hitbox[i];
      float cy = type.hitbox[i + 1];
      float s = type.hitbox[i + 2];

      // 正方形角点到中心的距离 (勾股定理的一半)
      float cornerDist = s / 2f * Mathf.sqrt2;

      // 中心点到原点的距离
      float centerDist = Mathf.len(cx, cy);

      // 最远点距离 = 中心距 + 角距
      float totalDist = centerDist + cornerDist;

      if (totalDist * totalDist > maxRadiusSq) {
        maxRadiusSq = totalDist * totalDist;
      }
    }

    // size 表示直径
    this.size = Mathf.sqrt(maxRadiusSq) * 2f;
  }

  @Override
  public void reset() {
    super.reset();
    this.type = null;
    this.speedX = 0;
    this.speedY = 0;
    this.targetX = 0;
    this.targetY = 0;
    this.rotation = 0;
    this.id = Entities.freeID(this.id);
    this.velocityDirty = true;

    this.isSelected = false;
    this.moving = false;
    this.hitboxData = null;

    this.pathFindCooldown = 0;
    if (path != null) path.clear();
  }

  @Override
  public void kill() {
    remove();
  }

  @Override
  public void remove() {
    WorldData.units.remove(this);
    Teams.remove(this);
    this.team = null;
    this.teamData = null;
    if (currentChunkIndex != -1
        && WorldData.unitGrid != null
        && currentChunkIndex < WorldData.unitGrid.length) {
      WorldData.unitGrid[currentChunkIndex].remove(this);
    }
    isSelected = false;
    currentChunkIndex = -1;
    Pools.free(this);
  }

  @Override
  public void update(float dt) {
    float oldX = this.x;
    float oldY = this.y;
    float oldRot = this.rotation;

    distToTarget = Mathf.dst(x, y, targetX, targetY);

    if (path == null && distToTarget < 2f) {
      x = targetX;
      y = targetY;
      distToTarget = 0f;
      speedX = 0;
      speedY = 0;
      moving = false;
    } else {
      x += speedX * dt;
      y += speedY * dt;
      moving = true;
    }

    if (distToTarget > 1f) {
      angleToTarget = Angles.angle(x, y, targetX, targetY);
    }

    if (canShoot) {
      if (mainFixedWeapon != null && distToTarget > 1f) {
        rotation = Angles.moveToward(rotation, angleToTarget - 90, rotationSpeed * dt);
      } else {
        if (Mathf.len(speedX, speedY) > 0.01f && distToTarget > 1f) {
          rotation = Angles.moveToward(rotation, angle - 90, rotationSpeed * dt);
        }
      }
    } else {
      if (Mathf.len(speedX, speedY) > 0.01f) {
        rotation = Angles.moveToward(rotation, angle - 90, rotationSpeed * dt);
      }
      type.update(this, dt);
    }

    moving = (x != oldX || y != oldY);
    boolean rotated = !Mathf.equal(rotation, oldRot);

    if (moving || rotated) {
      updateHitbox();
    }

    if (moving) {
      updateChunkPosition();
    }
  }

  private void updateHitbox() {
    if (hitboxData == null) return;

    if (type.hitbox != null) {
      float[] src = type.hitbox;
      float[] dst = hitboxData;

      for (int i = 0; i < src.length; i += 3) {
        float localX = src[i];
        float localY = src[i + 1];
        float boxSize = src[i + 2];

        Vec2 rotated = Tmp.v1.trns(rotation, localX, localY);

        dst[i] = x + rotated.x;
        dst[i + 1] = y + rotated.y;
        dst[i + 2] = boxSize;
      }
    } else {
      hitboxData[0] = x;
      hitboxData[1] = y;
      hitboxData[2] = size;
    }
  }

  public boolean contains(float px, float py) {
    if (hitboxData == null) return false;

    for (int i = 0; i < hitboxData.length; i += 3) {
      float cx = hitboxData[i];
      float cy = hitboxData[i + 1];
      float s = hitboxData[i + 2];

      float halfSize = s / 2f;

      if (px >= cx - halfSize
          && px <= cx + halfSize
          && py >= cy - halfSize
          && py <= cy + halfSize) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void draw() {
    type.draw(this);
  }

  public void drawDebug() {
    Draw.color(Color.yellow);
    Lines.stroke(2f);

    if (hitboxData != null) {
      for (int i = 0; i < hitboxData.length; i += 3) {
        float cx = hitboxData[i];
        float cy = hitboxData[i + 1];
        float s = hitboxData[i + 2];
        Lines.rect(cx - s / 2f, cy - s / 2f, s, s);
      }
    }

    // 绘制计算出的外接圆 (新增，用于验证 size 计算是否正确)
    Draw.color(Color.sky);
    float radius = size / 2f;
    Lines.circle(x, y, radius);
    Draw.color(Color.yellow); // 还原颜色

    if (Math.abs(speedX) > 0.001f || Math.abs(speedY) > 0.001f) {
      Draw.color(Color.magenta);
      float scale = 20f;
      Lines.line(x, y, x + speedX * scale, y + speedY * scale);
      Fonts.def.draw(Strings.format(speedX + " " + speedY), x, y + size + 8f, Align.center);
    }

    if (targetX != 0 || targetY != 0) {
      Draw.color(Color.orange);
      Lines.line(x, y, targetX, targetY);
      float s = 8f;
      Lines.line(targetX - s, targetY - s, targetX + s, targetY + s);
      Lines.line(targetX - s, targetY + s, targetX + s, targetY - s);
    }
    if (path != null && !path.isEmpty()) {
      Draw.color(Color.cyan);

      float lastX = x;
      float lastY = y;

      for (int i = pathIndex; i < path.size; i++) {
        Point2 p = path.get(i);
        float wx = p.x * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;
        float wy = p.y * WorldData.TILE_SIZE + WorldData.TILE_SIZE / 2f;

        Lines.line(lastX, lastY, wx, wy);
        Fill.square(wx, wy, 3f);
        lastX = wx;
        lastY = wy;
      }
    }

    Draw.color();
  }

  public void updateWeapons(float dt) {
    float aimX = targetX;
    float aimY = targetY;

    if (targetX == 0 && targetY == 0) {
      aimX = x + 100;
      aimY = y;
    }

    for (Weapon weapon : weapons) {
      weapon.update(dt, canShoot);
    }
  }

  private void updateChunkPosition() {
    if (WorldData.unitGrid == null) return;
    int newIndex = WorldData.getChunkIndex(x, y);
    if (newIndex < 0 || newIndex >= WorldData.unitGrid.length) return;

    if (newIndex != currentChunkIndex) {
      if (currentChunkIndex != -1 && currentChunkIndex < WorldData.unitGrid.length) {
        WorldData.unitGrid[currentChunkIndex].remove(this);
        if (teamData != null) {
          teamData.updateChunk(this, currentChunkIndex, newIndex);
        }
      }
      WorldData.unitGrid[newIndex].add(this);
      currentChunkIndex = newIndex;
    }
  }

  public void impuse(float knockX, float knockY) {
    this.x += knockX;
    this.y += knockY;
    this.velocityDirty = true;
  }

  @Override
  public void write(Writes w) {
    item.write(w);
    w.f(x);
    w.f(y);
    w.f(rotation);
    w.f(health);
    w.f(targetX);
    w.f(targetY);
    w.b(team.ordinal());
    w.s(id);
  }

  @Override
  public void read(Reads r) {
    item.read(r);
    this.x = r.f();
    this.y = r.f();
    this.rotation = r.f();
    this.health = r.f();
    this.targetX = r.f();
    this.targetY = r.f();
    byte teamId = r.b();
    this.id = Entities.checkoutID(r.s());

    if (teamId >= 0 && teamId < TeamTypes.values().length) {
      this.team = TeamTypes.values()[teamId];
    } else {
      this.team = TeamTypes.Abort;
    }

    Teams.add(this);

    teamData = team.data();
    teamData.updateChunk(this, -1, WorldData.getChunkIndex(x, y));

    this.speedX = 0;
    this.speedY = 0;

    updateTeamData();

    this.path = null;
    this.pathIndex = 0;
    this.pathed = false;
    this.velocityDirty = true;
    WorldData.moveunits.add(this);
    WorldData.units.add(this);

    updateHitbox();

    updateChunkPosition();
  }

  public void updateTeamData() {
    if (this.team == null) this.team = TeamTypes.Abort;
    this.teamData = this.team.data();
  }

  public void setTeam(TeamTypes newTeam) {
    if (this.team == newTeam) return;
    Teams.remove(this);
    this.team = newTeam;
    updateTeamData();
  }
}
