package caliniya.armavoke.type.ai;

import arc.math.Angles;
import arc.math.Mathf;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.Weapon;

/** 单位的通用指挥控制器，负责状态切换与移动攻击。 */
public class UnitAI {

  public enum State {
    Guard,
    Combat,
    HoldFire
  }

  public final Unit unit;
  public volatile State state = State.Combat;

  public UnitAI(Unit unit) {
    this.unit = unit;
  }

  /** 对象池复用时恢复默认战斗状态。 */
  public void reset() {
    state = State.Combat;
    unit.attackTarget = null;
    clearCombatTargets();
  }

  public boolean canTarget() {
    return state != State.HoldFire;
  }

  public void setState(State next) {
    if (next == null) return;
    state = next;

    if (next == State.Guard) {
      stop();
    } else if (next == State.HoldFire) {
      unit.attackTarget = null;
      clearCombatTargets();
    }
  }

  /** 普通移动指令会切回战斗状态并取消移动攻击目标。 */
  public void moveTo(float x, float y) {
    state = State.Combat;
    unit.attackTarget = null;
    unit.targetX = x;
    unit.targetY = y;
    unit.path = null;
    unit.pathIndex = 0;
    unit.pathed = false;
  }

  /** 移动攻击不进入寻路队列，单位会直线接近到交战距离。 */
  public void attack(Entity target) {
    if (!validEnemy(target)) return;
    state = State.Combat;
    unit.attackTarget = target;
    unit.path = null;
    unit.pathIndex = 0;
    unit.pathed = true;
    removeFromNavigation();
  }

  /** 停止当前移动，但不强制改变已有 AI 状态。 */
  public void stop() {
    unit.attackTarget = null;
    unit.speedX = 0f;
    unit.speedY = 0f;
    unit.targetX = unit.x;
    unit.targetY = unit.y;
    unit.path = null;
    unit.pathIndex = 0;
    unit.pathed = false;
    removeFromNavigation();
  }

  /** 在主线程更新移动攻击；普通寻路移动仍由 UnitMath 负责。 */
  public void update(float delta) {
    if (state == State.Guard) {
      unit.speedX = 0f;
      unit.speedY = 0f;
      return;
    }

    Entity target = unit.attackTarget;
    if (state != State.Combat || !validEnemy(target)) {
      if (target != null) {
        unit.attackTarget = null;
        unit.speedX = 0f;
        unit.speedY = 0f;
        unit.targetX = unit.x;
        unit.targetY = unit.y;
      }
      return;
    }

    unit.targetX = target.x;
    unit.targetY = target.y;
    float distance = Mathf.dst(unit.x, unit.y, target.x, target.y);
    float targetAngle = Angles.angle(unit.x, unit.y, target.x, target.y);
    unit.angle = targetAngle;

    if (distance <= unit.type.engageRange) {
      unit.speedX = 0f;
      unit.speedY = 0f;
    } else {
      unit.speedX = Mathf.cosDeg(targetAngle) * unit.speed;
      unit.speedY = Mathf.sinDeg(targetAngle) * unit.speed;
    }
  }

  private boolean validEnemy(Entity target) {
    return target != null
        && target.health > 0f
        && target.team != null
        && unit.team != null
        && target.team != unit.team;
  }

  private void clearCombatTargets() {
    unit.target = null;
    for (Weapon weapon : unit.weapons) {
      weapon.target = null;
    }
  }

  private void removeFromNavigation() {
    if (WorldData.moveunits == null) return;
    synchronized (WorldData.moveunits) {
      WorldData.moveunits.remove(unit);
    }
  }
}
