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
  public TeamTypes team;
  public TeamData teamData;

  public Ar<Weapon> weapons = new Ar<>();
  public Weapon mainFixedWeapon = null;

  // --- 物理属性 ---
  public float speedX, speedY, angle;
  public float rotationSpeed;
  public float rotation;
  public float angleToTarget, distToTarget;

  public boolean shooting = false;

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
  public int currentChunkIndex = -1;
  public float pathFindCooldown = 0f;

  // --- 碰撞体积缓存 (世界坐标) ---
  /** 存储旋转后的碰撞盒数据。 格式: [世界X1, 世界Y1, 尺寸1, 世界X2, 世界Y2, 尺寸2, ...] */
  public float[] hitboxData;

  protected Unit() {}

  public static Unit create(TeamTypes team, UnitType type, float x, float y) {
    Unit u = Pools.obtain(Unit.class, Unit::new);
    u.type = type;
    u.team = team;
    u.x = x;
    u.y = y;
    u.item = new ItemModule(100);
    u.init();
    return u;
  }

  public static Unit create(UnitType type, float x, float y) {
    return create(TeamTypes.Evoke, type, x, y);
  }

  public static Unit create(UnitType type) {
    return create(type, 100, 100);
  }

  public void init() {
    if (this.type == null) {
      this.type = UnitTypes.test;
      Log.err(this.toString() + "@ No unitTpye used test");
    }
    this.size = this.type.size;
    this.speed = this.type.speedt;
    this.rotationSpeed = this.type.rotationSpeend;
    this.region = this.type.region;
    this.cell = this.type.cell;

    this.maxHealth = this.type.health;
    this.health = this.type.health;

    // --- 初始化碰撞数据数组 ---
    if (type.hitbox != null) {
      hitboxData = new float[type.hitbox.length];
    } else {
      hitboxData = new float[3];
    }

    this.team = TeamTypes.Evoke;
    shooting = false;

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
    this.id = Entities.assignID();

    this.targetX = this.x;
    this.targetY = this.y;

    WorldData.units.add(this);
    updateChunkPosition();

    // 初始计算一次碰撞体
    updateHitbox();
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

    this.currentChunkIndex = -1;
    this.isSelected = false;
    this.moving = false;
    this.hitboxData = null; // 清空引用

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
    float oldRot = this.rotation; // 记录旧角度用于判断是否旋转

    distToTarget = Mathf.dst(x, y, targetX, targetY);

    // 到达目标点判定
    if (path == null && distToTarget < 2f) {
      x = targetX;
      y = targetY;
      distToTarget = 0f;
      speedX = 0;
      speedY = 0;
    } else {
      x += speedX * dt;
      y += speedY * dt;
    }

    if (distToTarget > 1f) {
      angleToTarget = Angles.angle(x, y, targetX, targetY);
    }

    // --- 旋转逻辑 ---
    if (shooting) {
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

    // --- 状态判断与碰撞更新优化 ---
    // 1. 判断是否移动 (位置发生变化)
    moving = (x != oldX || y != oldY);

    // 2. 判断是否旋转
    boolean rotated = !Mathf.equal(rotation, oldRot);

    // 3. 只有在移动或旋转时才重新计算碰撞体积
    // 注意：如果单位静止不动，碰撞体积数据保持不变，节省计算开销
    if (moving || rotated) {
      updateHitbox();
    }

    // 空间网格更新 (仅在移动时更新)
    if (moving) {
      updateChunkPosition();
    }
  }

  /** 根据当前位置和角度更新碰撞盒数据 */
  private void updateHitbox() {
    if (hitboxData == null) return;

    // 情况 1: 使用自定义碰撞体
    if (type.hitbox != null) {
      float[] src = type.hitbox;
      float[] dst = hitboxData;

      for (int i = 0; i < src.length; i += 3) {
        float localX = src[i];
        float localY = src[i + 1];
        float boxSize = src[i + 2];

        // 使用 Tmp.v1 计算旋转后的偏移量
        Vec2 rotated = Tmp.v1.trns(rotation, localX, localY);

        dst[i] = x + rotated.x;
        dst[i + 1] = y + rotated.y;
        dst[i + 2] = boxSize;
      }
    }
    // 情况 2: 默认正方形
    else {
      hitboxData[0] = x;
      hitboxData[1] = y;
      hitboxData[2] = size;
    }
  }

  /** 判断某个点是否在单位内部 */
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
    // 1. 绘制碰撞体积 (黄色)
    // 新的碰撞系统支持多方块，直接遍历 hitboxData 绘制即可
    Draw.color(Color.yellow);
    Lines.stroke(2f); // 稍微细一点的线条，适合多方块

    if (hitboxData != null) {
      for (int i = 0; i < hitboxData.length; i += 3) {
        float cx = hitboxData[i];
        float cy = hitboxData[i + 1];
        float s = hitboxData[i + 2];
        // 绘制以 为中心，边长为 s 的正方形
        Lines.rect(cx - s / 2f, cy - s / 2f, s, s);
      }
    }
    if (Math.abs(speedX) > 0.001f || Math.abs(speedY) > 0.001f) {
      Draw.color(Color.magenta);
      float scale = 20f;
      Lines.line(x, y, x + speedX * scale, y + speedY * scale);
      Fonts.def.draw(Strings.format(speedX + " " + speedY), x, y + size + 8f, Align.center);
    }

    // 3. 绘制目标点连接线 (橙色)
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

    Draw.color(); // 重置颜色
  }

  public void updateWeapons(float dt) {
    float aimX = targetX;
    float aimY = targetY;

    if (targetX == 0 && targetY == 0) {
      aimX = x + 100;
      aimY = y;
    }

    for (Weapon weapon : weapons) {
      weapon.update(dt, aimX, aimY, shooting);
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

    if (teamId >= 0 && teamId < TeamTypes.values().length) {
      this.team = TeamTypes.values()[teamId];
    } else {
      this.team = TeamTypes.Abort;
    }

    this.speedX = 0;
    this.speedY = 0;

    updateTeamData();

    this.path = null;
    this.pathIndex = 0;
    this.pathed = false;
    this.velocityDirty = true;
    WorldData.moveunits.add(this);

    updateChunkPosition();
  }

  public void updateTeamData() {
    if (this.team == null) this.team = TeamTypes.Abort;
    this.teamData = this.team.data();
    Teams.add(this);
  }

  public void updateTeamData(TeamTypes newTeam) {
    this.team = newTeam;
    updateTeamData();
  }

  public void setTeam(TeamTypes newTeam) {
    if (this.team == newTeam) return;
    Teams.remove(this);
    this.team = newTeam;
    updateTeamData();
  }
}
