package caliniya.armavoke.type.type;

import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.Core;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.game.Unit;

public class BulletType {

  public float speed = 6f;
  public float damage = 10f;
  public float lifetime = 600f; // 存活帧数
  public float hitW = 20f;
  public float hitH = 20f;
  
  // 渲染相关
  public float drawSize = 1f; // 整体缩放比例 (可选)
  public Color frontColor = Color.white; // 子弹前景色
  public Color backColor = Color.gray;   // 子弹背景色 (如果有双层绘制)

  public TextureRegion region;

  // 子弹类型：0=常规，1=激光(射线检测)，2=导弹(追踪)...
  // 也可以通过继承 BulletType 来实现多态

  public BulletType() {
  }

  public void load() {
    // 加载纹理，通常是 "weaponName-bullet"
    this.region = Core.atlas.find("bullet");
  }

  /** 子弹更新逻辑 (每帧调用) */
  public void update(Bullet b) {
    // 默认直线飞行
    // 子弹位置更新通常在 BulletSystem 统一处理以获得最佳性能
    // 这里可以处理特效、追踪逻辑等
  }

  /** 子弹绘制逻辑 */
  public void draw(Bullet b) {
    if (region == null) return;

    float drawW = hitW; 
    float drawH = hitH; 

    // 1. 绘制背层 (光晕)
    Draw.color(backColor);
    Draw.rect(region, b.x, b.y, drawW * 1.5f, drawH * 1.5f, b.rotation - 90);

    // 2. 绘制前层 (核心)
    Draw.color(frontColor);
    Draw.rect(region, b.x, b.y, drawW, drawH, b.rotation - 90);
    
    Draw.color(); // 重置
  }

  /** 命中单位时的回调 */
  public void hit(Bullet b, Unit target) {
    if (target != null) {
      target.health -= this.damage;
      // TODO: 播放命中特效
    }
    b.remove(); // 销毁子弹
  }

  /** 命中墙壁/消失时的回调 */
  public void despawn(Bullet b) {
    // TODO: 播放消失特效
    b.remove();
  }
}
