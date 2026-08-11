package caliniya.armavoke.system.render;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.type.*;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.base.tool.*;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.type.Bullet;
import caliniya.armavoke.type.ability.ShieldAbility;
import caliniya.armavoke.type.type.BulletType;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;
import caliniya.armavoke.system.world.BulletProcess;
import caliniya.armavoke.ui.fragment.UniverseFragment;

public class UnitRender extends System<UnitRender> {

  // 调试开关
  public static boolean debug = true;

  /** 血条样式：5 = 固定分段 + 段内独立填充（最终版，默认）；1~4 为旧样式。 */
  public static int barStyle = 5;

  public static Ar<Bullet> temp = new Ar<Bullet>(false, 1000);

  @Override
  public UnitRender init() {
    this.index = 13;
    Events.run(EventType.events.EnterUV, () -> paused = true);
    Events.run(EventType.events.ExitUV, () -> paused = false);
    return super.init(false,false);
  }

  @Override
  public void update() {
    if (!inited || paused) return;
    // 绘制单位
    WorldData.units.each(
        u -> {
          if (shouldDraw(u.x, u.y, u.size * 2)) {
            u.draw();
            // 整合血条（核心/护甲/护盾）
            if (barStyle == 2) {
              drawHealthBar2(u);
            } else if (barStyle == 3) {
              drawHealthBar3(u);
            } else if (barStyle == 4) {
              drawHealthBar4(u);
            } else if (barStyle == 5) {
              drawHealthBar5(u);
            } else {
              drawHealthBar(u);
            }
            // 调用单位内部的调试绘制方法
            if (debug) {
              u.type.drawDebug(u);
            }
          }
        });

    // 绘制子弹
    // 用与 BulletProcess 相同的固定锁对象，确保与逻辑线程的缓冲交换互斥，
    // 避免拷到正在被清空/重填的缓冲导致子弹闪烁。
    temp.clear();
    synchronized (Systems.BP.BULLET_LOCK) {
      temp.addAll(WorldData.bullets);
    }
    temp.each(
        b -> {
          if (shouldDraw(b.x, b.y, b.type.size)) {
            b.type.draw(b);
          }
        });
  }

  /**
   * 整合血条：一个血条按三层容量比例分段，从左到右依次为
   * 核心（红）、护甲（白）、护盾（蓝），每段按当前值填充。
   *
   * <p>各段宽度 = 该层最大容量占总容量的比例；
   * 例如核心/护甲/护盾容量相等时各占 1/3，护甲空（容量 0）时核心与护盾各占 1/2。
   */
  private void drawHealthBar(Unit u) {
    ShieldAbility shield = u.shield();
    float coreMax = Math.max(0f, u.maxHealth);
    float core = Math.max(0f, u.health);
    float armorMax = Math.max(0f, u.armorMax);
    float armor = Math.max(0f, u.armor);
    float shieldMax = shield == null ? 0f : Math.max(0f, shield.max);
    float shieldCur = shield == null ? 0f : Math.max(0f, shield.current);

    float totalMax = coreMax + armorMax + shieldMax;
    if (totalMax <= 0f) return;

    // 血条尺寸与位置（单位正下方）
    float barW = 64f;
    float barH = 6f;
    float x = u.x - barW / 2f;
    float y = u.y - u.size / 2f - barH - 6f;

    // 各段宽度（按容量比例）
    float coreW = barW * coreMax / totalMax;
    float armorW = barW * armorMax / totalMax;
    float shieldW = barW * shieldMax / totalMax;

    // 底色
    Draw.color(Color.darkGray);
    Fill.rect(x + barW / 2f, y + barH / 2f, barW, barH);

    // 核心段（最左，红）
    if (coreW > 0f && core > 0f) {
      float w = coreW * (core / coreMax);
      Draw.color(Color.scarlet);
      Fill.rect(x + w / 2f, y + barH / 2f, w, barH);
    }

    // 护甲段（中，白）
    if (armorW > 0f && armor > 0f) {
      float w = armorW * (armor / armorMax);
      Draw.color(Color.lightGray);
      Fill.rect(x + coreW + w / 2f, y + barH / 2f, w, barH);
    }

    // 护盾段（最右，蓝）
    if (shieldW > 0f && shieldCur > 0f) {
      float w = shieldW * (shieldCur / shieldMax);
      Draw.color(Color.sky);
      Fill.rect(x + coreW + armorW + w / 2f, y + barH / 2f, w, barH);
    }

    Draw.color();
  }

  /**
   * 整合血条第 2 种样式：各段**永远整段填充**，
   * 段宽 = 该层剩余容量 / 三层**当前剩余总容量**，
   * 三段正好填满整条血条（永远完全填充，无底色空隙）。
   *
   * <p>例如 核心 500、护甲 300、护盾 200（剩余总 1000）：
   * 核心段 500/1000、护甲段 300/1000、护盾段 200/1000，正好占满整条。
   */
  private void drawHealthBar2(Unit u) {
    ShieldAbility shield = u.shield();
    float coreMax = Math.max(0f, u.maxHealth);
    float core = Math.max(0f, u.health);
    float armorMax = Math.max(0f, u.armorMax);
    float armor = Math.max(0f, u.armor);
    float shieldMax = shield == null ? 0f : Math.max(0f, shield.max);
    float shieldCur = shield == null ? 0f : Math.max(0f, shield.current);

    // 当前剩余总容量（三段之和）
    float totalCur = core + armor + shieldCur;
    if (totalCur <= 0f) return;

    float barW = 64f;
    float barH = 6f;
    float x = u.x - barW / 2f;
    float y = u.y - u.size / 2f - barH - 6f;

    // 底色（整条）
    Draw.color(Color.darkGray);
    Fill.rect(x + barW / 2f, y + barH / 2f, barW, barH);

    // 各段宽度 = 该层剩余容量 / 当前剩余总容量（三段正好填满 barW）
    float coreW = barW * core / totalCur;
    float armorW = barW * armor / totalCur;
    float shieldW = barW * shieldCur / totalCur;

    // 每段整段填充（不显示内部比例）
    if (coreW > 0f) {
      Draw.color(Color.scarlet);
      Fill.rect(x + coreW / 2f, y + barH / 2f, coreW, barH);
    }
    if (armorW > 0f) {
      Draw.color(Color.lightGray);
      Fill.rect(x + coreW + armorW / 2f, y + barH / 2f, armorW, barH);
    }
    if (shieldW > 0f) {
      Draw.color(Color.sky);
      Fill.rect(x + coreW + armorW + shieldW / 2f, y + barH / 2f, shieldW, barH);
    }

    Draw.color();
  }

  /**
   * 整合血条第 3 种样式：血条只画**护甲 + 核心**两段；
   * 护盾按单位的 {@code size} 画成**环绕圆环**（能量护盾感）。
   *
   * <p>圆环比例 = 当前等效护盾 / 最高等效护盾：
   *
   * <pre>
   * p = 当前容量 / 最大容量
   * 当前强度 = p × 最大护盾强度
   * 当前等效 = 当前容量 × 当前强度
   * 最高等效 = 最大容量 × 最大护盾强度
   * 比例 = 当前等效 / 最高等效 = p²
   * </pre>
   *
   * 满盾画满圆，半盾画 1/4 圆（残盾等效护盾低，圆环明显变少）。
   */
  private void drawHealthBar3(Unit u) {
    ShieldAbility shield = u.shield();

    // 血条：护甲 + 核心（两段）
    float coreMax = Math.max(0f, u.maxHealth);
    float core = Math.max(0f, u.health);
    float armorMax = Math.max(0f, u.armorMax);
    float armor = Math.max(0f, u.armor);

    float totalMax = coreMax + armorMax;
    if (totalMax > 0f) {
      float barW = 64f;
      float barH = 6f;
      float x = u.x - barW / 2f;
      float y = u.y - u.size / 2f - barH - 6f;

      float coreW = barW * coreMax / totalMax;
      float armorW = barW * armorMax / totalMax;

      Draw.color(Color.darkGray);
      Fill.rect(x + barW / 2f, y + barH / 2f, barW, barH);

      if (coreW > 0f && core > 0f) {
        float w = coreW * (core / coreMax);
        Draw.color(Color.scarlet);
        Fill.rect(x + w / 2f, y + barH / 2f, w, barH);
      }
      if (armorW > 0f && armor > 0f) {
        float w = armorW * (armor / armorMax);
        Draw.color(Color.lightGray);
        Fill.rect(x + coreW + w / 2f, y + barH / 2f, w, barH);
      }
    }

    // 护盾环绕圆环（空心圆弧）
    if (shield != null && shield.max > 0f && shield.current > 0f) {
      float p = shield.current / shield.max;
      float strength = p * shield.maxStrength;
      float equiv = shield.current * strength;
      float maxEquiv = shield.max * shield.maxStrength;
      float ratio = maxEquiv <= 0f ? 0f : equiv / maxEquiv;

      float radius = u.size / 2f + 10f;
      Lines.stroke(3f, Color.sky);
      Lines.arc(u.x, u.y, radius, Math.max(0.05f, ratio), 90f, 48);
      Lines.stroke(1f);
    }
  }

  /**
   * 最终血条样式：单位右边缘的垂直条。
   *
   * <p>以单位正中心为原点，起点 (size, -size)，向上长 size×1.5（size 为直径）。
   * 从下到上依次为核心（红）、护甲（白）、护盾（蓝），**完全填满**样式：
   * 各段高度 = 条长 × (该层当前容量 / 当前总容量)。
   *
   * <p>护盾段使用**平均容量** = (原始容量 + 等效容量) / 2，
   * 其中等效容量 = 原始容量 × 最大护盾强度（固定系数），用于总容量计算与护盾段显示。
   */
  private void drawHealthBar4(Unit u) {
    ShieldAbility shield = u.shield();

    // 各层当前容量
    float core = Math.max(0f, u.health);
    float armor = Math.max(0f, u.armor);

    // 护盾平均容量
    float shieldAvg = 0f;
    if (shield != null && shield.max > 0f) {
      float cur = Math.max(0f, shield.current);
      float equiv = cur * shield.maxStrength; // 等效容量 = 原始容量 × 最大护盾强度
      shieldAvg = (cur + equiv) / 2f;
    }

    float total = core + armor + shieldAvg;
    if (total <= 0f) return;

    // 血条几何：起点 (size, -size)，向上长 size×1.5（size 为直径）
    float barLen = u.size * 1.5f;
    float barW = 6f;
    float startX = u.x + u.size;
    float startY = u.y - u.size;

    // 底色
    Draw.color(Color.darkGray);
    Fill.rect(startX + barW / 2f, startY + barLen / 2f, barW, barLen);

    // 各段高度（完全填满：比例 = 该层容量 / 总容量）
    float coreH = barLen * core / total;
    float armorH = barLen * armor / total;
    float shieldH = barLen * shieldAvg / total;

    // 核心段（最下，红）
    if (coreH > 0f) {
      Draw.color(Color.scarlet);
      Fill.rect(startX + barW / 2f, startY + coreH / 2f, barW, coreH);
    }
    // 护甲段（中，白）
    if (armorH > 0f) {
      Draw.color(Color.lightGray);
      Fill.rect(startX + barW / 2f, startY + coreH + armorH / 2f, barW, armorH);
    }
    // 护盾段（最上，蓝）
    if (shieldH > 0f) {
      Draw.color(Color.sky);
      Fill.rect(startX + barW / 2f, startY + coreH + armorH + shieldH / 2f, barW, shieldH);
    }

    Draw.color();
  }

  /**
   * 最终血条样式：单位右边缘垂直条，**固定分段 + 段内独立填充**。
   *
   * <p>1. 固定分段：总容量 = 核心Max + 护甲Max + 护盾原始Max，
   *      各段宽 = 条长 × 该层Max/总容量（段位置固定，互不影响）；
   * <p>2. 段内独立渲染：每段从左到右按「当前 / 该层最大」填充，
   *      左侧固定、右侧随当前值缩（留出底色）；
   * <p>3. 护盾段比例用等效容量：等效 = 当前 × 最大护盾强度，
   *      比例 = 等效 / 最大等效 = 当前/最大（接近线性）。
   */
  private void drawHealthBar5(Unit u) {
    ShieldAbility shield = u.shield();

    float coreMax = Math.max(0f, u.maxHealth);
    float core = Math.max(0f, u.health);
    float armorMax = Math.max(0f, u.armorMax);
    float armor = Math.max(0f, u.armor);
    float shieldMax = shield == null ? 0f : Math.max(0f, shield.max);
    float shieldCur = shield == null ? 0f : Math.max(0f, shield.current);

    // 固定分段：总容量用护盾字面最大容量
    float totalMax = coreMax + armorMax + shieldMax;
    if (totalMax <= 0f) return;

    // 血条几何（右侧垂直条）
    float barLen = u.size * 1.5f;
    float barW = 6f;
    float startX = u.x + u.size;
    float startY = u.y - u.size;

    // 底色
    Draw.color(Color.darkGray);
    Fill.rect(startX + barW / 2f, startY + barLen / 2f, barW, barLen);

    // 各段宽（固定）
    float coreSeg = barLen * coreMax / totalMax;
    float armorSeg = barLen * armorMax / totalMax;
    float shieldSeg = barLen * shieldMax / totalMax;

    // 核心段（最下，红）：段内按当前/最大填充
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

    // 护盾段（最上，蓝）：用等效容量比例（= 当前/最大，线性）
    if (shieldSeg > 0f && shieldCur > 0f) {
      float h = shieldSeg * (shieldCur / shieldMax);
      Draw.color(Color.sky);
      Fill.rect(startX + barW / 2f, startY + coreSeg + armorSeg + h / 2f, barW, h);
    }

    Draw.color();
  }

  // 通用的剔除方法
  private boolean shouldDraw(float x, float y, float size) {
    float viewX = Core.camera.position.x;
    float viewY = Core.camera.position.y;
    float buffer = debug ? 500f : size;
    float w = Core.camera.width / 2f + buffer;
    float h = Core.camera.height / 2f + buffer;
    return x > viewX - w && x < viewX + w && y > viewY - h && y < viewY + h;
  }
}
