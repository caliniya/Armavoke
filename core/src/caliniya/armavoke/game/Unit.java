package caliniya.armavoke.game;

import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.struct.Seq;
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
import caliniya.armavoke.game.type.WeaponType;

public class Unit implements Poolable {

  public UnitType type;
  public int id;
  public TeamTypes team;
  public TeamData teamData;

  public Ar<Weapon> weapons = new Ar<>();

  // --- 物理属性 ---
  public float x, y;
  public float speedX, speedY, angle , // 速度分量 (每帧移动的像素量),速度方向
  rotationSpeend
  ; 
  public float rotation; // 渲染朝向 (度)
  
  public boolean shooting;

  // --- 导航属性 ---
  public float targetX, targetY;
  public Ar<Point2> path;
  public int pathIndex = 0;
  public boolean pathed; // 真则当前已经请求过一次导航数据
  public boolean velocityDirty = true; // 方向请求否

  // --- 状态属性 ---
  public boolean isSelected = false;
  public float health, w, h, speed; // speed 这里指最大标量速度，单位像素每帧(单位类型中同名的实际上是格每秒，从speedt(像素每帧)中获取)
  public TextureRegion region, cell;
  public int currentChunkIndex = -1;
  public float pathFindCooldown = 0f;

  protected Unit() {}

  public static Unit create(TeamTypes team, UnitType type, float x, float y) {
    Unit u = Pools.obtain(Unit.class, Unit::new);
    u.type = type;
    u.team = team;
    u.init();
    u.x = x;
    u.y = y;
    return u;
  }

  public static Unit create(UnitType type , float x , float y) {
    return create(TeamTypes.Evoke ,type , x , y);
    // TODO: 用于回调到玩家阵营的创建方法,需要删除
  }
  
  public static Unit create(UnitType type) {
  	return create(type , 500 , 500);
  }

  public void init() {
    if (this.type == null) {
      this.type = UnitTypes.test;
      Log.err(this.toString() + "@ No unitTpye used test");
    }
    this.w = this.type.w;
    this.h = this.type.h;
    this.speed = this.type.speedt;
    this.rotationSpeend = this.type.rotationSpeend;
    this.region = this.type.region;
    this.cell = this.type.cell;

    this.health = this.type.health;

    this.team = TeamTypes.Evoke;

    weapons.clear();
    for (WeaponType wType : type.weapons) {
      weapons.add(new Weapon(wType, this));
    }

    this.rotation = 0f;
    this.speedX = 0f;
    this.speedY = 0f;
    this.id = (new Rand().random(10000));

    // 初始化目标为当前位置，防止刚出生就归零
    this.targetX = this.x;
    this.targetY = this.y;

    // 加入世界列表
    WorldData.units.add(this);

    // 立即更新一次网格位置，确保出生就能被点中
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

  /** 更新单位在网格中的位置 */
  private void updateChunkPosition() {
    if (WorldData.unitGrid == null) return;

    int newIndex = WorldData.getChunkIndex(x, y);

    if (newIndex < 0 || newIndex >= WorldData.unitGrid.length) return;

    if (newIndex != currentChunkIndex) {
      if (currentChunkIndex != -1 && currentChunkIndex < WorldData.unitGrid.length) {
        WorldData.unitGrid[currentChunkIndex].remove(this);
      }
      WorldData.unitGrid[newIndex].add(this);
      currentChunkIndex = newIndex;
    }
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

  public void update() {}

  public void updateWeapons() {
    for (Weapon weapon : weapons) {
      weapon.update();
    }
  }

  // TODO: 不能用
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

    // 重置寻路状态
    this.path = null;
    this.pathIndex = 0;
    this.pathed = false;
    this.velocityDirty = true;
    WorldData.moveunits.add(this); // 加入以强制应用导航数据

    // 立即更新网格位置
    updateChunkPosition();
  }

  /** 更新团队数据引用并注册(不带团队参数) */
  public void updateTeamData() {
    if (this.team == null) this.team = TeamTypes.Abort; // 默认中立

    this.teamData = this.team.data();
    Teams.add(this);
  }

  public void updateTeamData(TeamTypes newTeam) {
    this.team = newTeam;
    updateTeamData();
  }

  public void setTeam(TeamTypes newTeam) {
    if (this.team == newTeam) return;

    // 从旧团队移除
    Teams.remove(this);
    this.team = newTeam;

    // 加入新团队
    updateTeamData();
  }
}
