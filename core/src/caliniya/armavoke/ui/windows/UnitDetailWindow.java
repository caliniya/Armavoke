package caliniya.armavoke.ui.windows;

import arc.func.Floatf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.ui.Button;

/** 单位详细信息窗口： 类型信息（名字 + DataWindow 按钮 + stats meta）、 血量/能量/护盾（数字 + 条形图，实时更新）、能力列表（可开关能力带开关按钮）。 */
public class UnitDetailWindow extends Window {

  private final Unit unit;

  public UnitDetailWindow(Unit unit) {
    super(unit.type.name);
    this.unit = unit;
  }

  @Override
  public void main(Table t) {
    if (unit == null) return;
    t.clearChildren();

    // 名字 + "类型信息"按钮（复用 DataWindow 展示 StatStack）
    Table nameRow = new Table();
    nameRow.left();
    nameRow.add("[light]" + unit.type.name + "[]").left().pad(2f);
    nameRow
        .add(new Button("类型信息", () -> new DataWindow(unit.type.stats()).build()))
        .size(84f, 36f)
        .padLeft(8f);
    t.add(nameRow).growX().left().row();

    // 实例属性（数字 + 条形图，每帧实时刷新）
    addStat(t, "血量", u -> u.health, u -> u.maxHealth, Color.scarlet);
    addStat(t, "护盾", u -> u.totalShield(), u -> u.totalShieldMax(), Color.sky);
    addStat(t, "护甲", u -> u.armor, u -> u.armorMax, Color.lightGray);
    addStat(t, "能量", u -> u.energy, u -> u.energyMax, Color.gold);
    t.add().height(8f).row();

    // 能力列表
    t.add("[light]能力[]").left().pad(2f).row();
    if (unit.abilities.size == 0) {
      t.add("[gray]无能力[]").left().pad(2f).row();
    } else {
      for (Ability a : unit.abilities) {
        Table row = new Table();
        row.left();
        row.add("[gray]" + a.getClass().getSimpleName() + "[]").left().pad(2f);
        if (a.toggleable) {
          row.add(
                  new Button(
                      a.enabled ? "关闭" : "开启",
                      () -> {
                        a.setEnabled(!a.enabled);
                        main(this.main); // 刷新窗口内容
                      }))
              .size(64f, 36f)
              .padLeft(6f);
        } else {
          row.add("[gray]被动[]").padLeft(6f);
        }
        t.add(row).growX().left().row();
      }
    }
  }

  /** 属性行：名称 + 数字 + 条形图，每帧更新。 */
  private void addStat(Table t, String name, Floatf<Unit> cur, Floatf<Unit> max, Color color) {
    Table row = new Table();
    row.left();
    row.add("[gray]" + name + "[]").width(50f).left();
    Label value = new Label("");
    row.add(value).width(90f).left();
    Element barEl = bar(cur, max, color);
    row.add(barEl).size(120f, 8f).left();
    t.add(row).growX().left().row();

    // 每帧刷新数值与条形图
    value.update(() -> value.setText((int) cur.get(unit) + "/" + (int) max.get(unit)));
  }

  /** 条形图元素：每帧读取最新值绘制。 */
  private Element bar(Floatf<Unit> cur, Floatf<Unit> max, Color color) {
    float[] curVal = {0f};
    float[] maxVal = {1f};
    return new Element() {
      {
        setSize(120f, 8f);
        update(
            () -> {
              curVal[0] = cur.get(unit);
              maxVal[0] = max.get(unit);
            });
      }

      @Override
      public void draw() {
        float x = this.x;
        float y = this.y;
        float w = getWidth();
        float h = getHeight();

        Draw.color(Color.darkGray);
        Fill.rect(x + w / 2f, y + h / 2f, w, h);
        if (maxVal[0] > 0f) {
          float fw = w * Math.min(1f, curVal[0] / maxVal[0]);
          Draw.color(color);
          Fill.rect(x + fw / 2f, y + h / 2f, fw, h);
        }
        Draw.color();
      }
    };
  }
}
