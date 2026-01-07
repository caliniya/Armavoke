package caliniya.armavoke.game;

import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pool.Poolable;
import arc.util.pooling.Pools;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.content.UnitTypes;
import caliniya.armavoke.game.data.*;
import caliniya.armavoke.game.type.UnitType;
import caliniya.armavoke.core.*;
import caliniya.armavoke.type.*;
import caliniya.armavoke.type.type.*;

public class Unit implements Poolable {

  public UnitType type;
  public int id;
  public TeamTypes team;
  public TeamData teamData;

  public Ar<Weapon> weapons = new Ar<>();
  public Weapon mainFixedWeapon = null; // 缓存的主固定武器

  // --- 物理属性 ---
  public float x, y;
  public float speedX, speedY, angle; // 速度分量 (每帧移动的像素量), 速度方向
  public float rotationSpeed;
  public float rotation; // 渲染朝向 (度)
  public float angleToTarget, distToTarget;

  public boolean shooting = false;

  // --- 导航属性 ---
  public float targetX, targetY;
  public Ar<Point2> path;
  public int pathIndex = 0;
  public boolean pathed; // 真则当前已经请求过一次导航数据
  public boolean velocityDirty = true; // 方向请求否

  // --- 状态属性 ---
  public boolean isSelected = false;
  public float health, size, speed; // speed 这里指最大标量速度
  public TextureRegion region, cell;
  public int currentChunkIndex = -1;
  public float pathFindCooldown = 0f;

  protected Unit() {}

  public static Unit create(TeamTypes team, UnitType type, float x, float y) {
    Unit u = Pools.obtain(Unit.class, Unit::new);
    u.type = type;
    u.team = team;
    u.x = x;
    u.y = y;
    u.init();
    return u;
  }

  public static Unit create(UnitType type, float x, float y) {
    return create(TeamTypes.Evoke, type, x, y);
  }

  public static Unit create(UnitType type) {
    return create(type, 500, 500);
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

    this.health = this.type.health;

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
    this.id = (new Rand().random(10000));

    this.targetX = this.x;
    this.targetY = this.y;

    WorldData.units.add(this);
    updateChunkPosition();
  }

  @Override
  public void reset() {
    this.type = null;
    this.x = 0;
    this.y = 0;
    this.speedX = 0;
    this.speedY = 0;
    this.targetX = 0;
    this.targetY = 0;
    this.rotation = 0;
    this.health = 0;
    this.id = -1;
    this.velocityDirty = true;

    this.currentChunkIndex = -1;
    this.isSelected = false;
    this.pathFindCooldown = 0;
    if (path != null) path.clear();
  }

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

  /**
   * 单位更新逻辑
   *
   * @param dt 时间增量
   */
  public void update(float dt) {
    float oldX = this.x;
    float oldY = this.y;
    
    distToTarget = Mathf.dst(x, y, targetX, targetY);
    
    if (path == null && distToTarget < 2f) {
      x = targetX;
      y = targetY;
      distToTarget = 0f; 
    } else {
      x += speedX * dt;
      y += speedY * dt;
    }
    
    if (distToTarget > 1f) { 
        angleToTarget = Angles.angle(x, y, targetX, targetY);
    }
    
    if (shooting) {
      // 射击模式下
      if (mainFixedWeapon != null && distToTarget > 1f) {
        
        rotation = Angles.moveToward(rotation, angleToTarget - 90, rotationSpeed * dt);

      } else {
        if (Mathf.len(speedX, speedY) > 0.01f && distToTarget > 1f) {
          rotation = Angles.moveToward(rotation, angle - 90, rotationSpeed * dt);
        }
      }
    } else {
      // 非射击模式：始终朝向移动方向
      if (Mathf.len(speedX, speedY) > 0.01f) {
        rotation = Angles.moveToward(rotation, angle - 90, rotationSpeed * dt);
      }
    }

    // 空间网格更新
    if (x != oldX || y != oldY) {
      updateChunkPosition();
    }
  }

  /**
   * 武器逻辑更新
   *
   * @param dt 时间增量
   */
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

  public void write(Writes w) {
    w.f(x);
    w.f(y);
    w.f(rotation);
    w.f(health);
    w.f(targetX);
    w.f(targetY);
    w.b(team.ordinal());
  }

  public void read(Reads r) {
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
