package caliniya.armavoke.type.ability;

import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.core.meta.stat.Stat;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.core.meta.stat.StatUnit;

/**
 * 过热能力：负责<b>散热</b>与<b>锁定</b>。热量由外部热源（武器/能力/模组）通过 {@code Entity.addHeat} 添加； 达到储热上限 →
 * 锁定单位，热量归零后恢复。储热上限为 0 = 无过热机制。
 *
 * <p>没附加此能力的单位完全无过热机制（能力系统：附加即生效）。
 */
public class HeatAbility extends Ability {

  /** 最大储热上限（达到即过热锁定；0 = 无过热机制）。 */
  public float heatMax = 100f;

  /** 散热速度（每秒降低的热量）。 */
  public float heatSpeed = 20f;

  /** 当前热量。 */
  public float heat;
  
  @Override
  public Ability onCreate(Entity e) {
    e.heatSpeed = heatSpeed;
    e.heatable = true;
    e.heatMax = heatMax;
    return super.onCreate(e);
  }
  

  public HeatAbility() {
    super("heat");
  }

  public HeatAbility(float heatMax) {
    super("heat");
    this.heatMax = heatMax;
  }
  
  // 对于过热锁定 只能在这里进行
  @Override
  public void update(Entity e, float dt) {
    heat = e.heat;
    if(heat > heatMax) {
    	e.locked = true;
    }
  }

  /** 当前热量比例（0~1），用于热量条显示。 */
  public float percent() {
    return heatMax <= 0f ? 0f : heat / heatMax;
  }

  @Override
  public void write(Entity e ,Writes w) {
    super.write(w);
    w.f(e.heat);
  }

  @Override
  public void read(Entity e ,Reads r) {
    super.read(r);
    heat = r.f();
    e.locked = heat >= heatMax;
  }

  @Override
  public void stats(StatStack stack) {
    stack.add(Stat.heat, heatMax, StatUnit.none, localizedName);
    stack.add(Stat.heatSpeed, heatSpeed, StatUnit.perSecond, localizedName);
  }
}
