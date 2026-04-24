package caliniya.armavoke.game;

import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.util.Log;
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
  public float size, speed; 
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
    u.item = new ItemModule(100);
    u.init();
    return u;
  }

  public static Unit create(UnitType type, float x, float y) {
    return create(TeamTypes.Evoke, type, x, y);
  }
  
  public static Unit create(UnitType type) {
    return create(type , 100 , 100);
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

    // 初始化基类血量
    this.maxHealth = this.type.health;
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
    this.id = Entities.assignID();

    this.targetX = this.x;
    this.targetY = this.y;

    WorldData.units.add(this);
    updateChunkPosition();
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
    this.pathFindCooldown = 0;
    if (path != null) path.clear();
  }
  
  @Override
  public void kill() {
    // TODO: Implement this method
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
      type.update(this,dt);
    }

    if (x != oldX || y != oldY) {
      updateChunkPosition();
    }
  }
  
  @Override
  public void draw(){
    type.draw(this);
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