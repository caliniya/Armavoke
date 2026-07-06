package caliniya.armavoke.world.defence.turret;

import arc.math.Angles;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.Core;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.io.Writes;
import arc.util.io.Reads;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Entities; // 导入 Entities 工具类
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.type.BulletType;
import caliniya.armavoke.world.Block;

public class Turret extends Block {

  // --- 炮塔属性 ---
  public float range = 200f; // 攻击范围 (像素)
  public float rotateSpeed = 500f; // 炮管旋转速度
  public float reloadTime = 10f; // 射击冷却 (帧)
  public BulletType bulletType; // 使用的子弹类型

  // --- 渲染 ---
  public TextureRegion baseRegion; // 基座贴图

  public Turret(String name) {
    super(name);
    this.capacity = 0;
  }

  @Override
  public void load() {
    super.load();
    // 加载基座和炮管贴图
    baseRegion = Core.atlas.find(name + "-base");
    // region 继承自 Block，默认查找 name，这里作为炮管
    if (bulletType != null) bulletType.load();
  }

  @Override
  public void update(Building b, float dt) {
    // 1. 目标检测逻辑
    // 如果当前目标无效，使用 Entities 接口寻找新目标
    if (b.target == null || !isValidTarget(b, b.target)) {
      b.target = findTarget(b);
    }

    // 2. 瞄准逻辑
    if (b.target != null) {
      // 计算目标角度 (建筑中心 -> 目标)
      float targetAngle = Angles.angle(b.x + psize / 2, b.y + psize / 2, b.target.x, b.target.y);

      // 平滑旋转炮管
      b.rotation = Angles.moveToward(b.rotation, targetAngle, rotateSpeed * dt);

      // 3. 射击逻辑
      b.reload += dt;

      // 判断是否可以射击：装填完毕 且 角度对准 (误差小于5度)
      if (b.reload >= reloadTime && Angles.angleDist(b.rotation, targetAngle) < 5f) {
        shoot(b, b.rotation);
        b.reload = 0;
      }
    }
  }

  @Override
  public void draw(Building b) {
    // 1. 绘制底座 (跟随建筑放置角度 angle * 90)
    Draw.rect(baseRegion, b.x + psize / 2, b.y + psize / 2, b.angle * 90f);

    // 2. 绘制炮管 (跟随实时旋转角度 rotation)
    if (region != null) {
      Draw.rect(region, b.x + psize / 2, b.y + psize / 2, b.rotation - 90f);
    }
  }

  /** 射击实现 */
  private void shoot(Building b, float angle) {
    float x = b.x + psize / 2;
    float y = b.y + psize / 2;

    // 调用 Bullet.create
    Bullet.create(bulletType, b, x, y, angle, 0, 0);
  }

  /** 寻找目标：使用全局索敌接口 */
  private Unit findTarget(Building b) {
    // 使用 Entities.closestEnemy 进行索敌
    // 参数：己方阵营, 中心X, 中心Y, 搜索半径
    return Entities.closestEnemy(b.team, b.x + psize / 2, b.y + psize / 2, range);
  }

  /** 验证目标有效性 */
  private boolean isValidTarget(Building b, Unit target) {
    if (target == null || target.health <= 0) return false;
    // 检查距离是否还在范围内
    float dst = Mathf.dst2(b.x, b.y, target.x, target.y);
    return dst <= range * range;
  }

  @Override
  public void write(Building b, Writes w) {
    w.f(b.rotation); // 炮管角度
    w.f(b.reload); // 装填进度
  }

  @Override
  public void read(Building b, Reads r) {
      b.rotation = r.f();
      b.reload = r.f();
  }
}
