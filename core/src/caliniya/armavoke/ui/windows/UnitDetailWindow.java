package caliniya.armavoke.ui.windows;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.ui.Button;

/** 单位详细信息窗口： 类型 meta、血量/能量/护盾（数字 + 条形图）、能力列表（可开关能力带开关按钮）。 */
public class UnitDetailWindow extends Window {

  private final Unit unit;

  public UnitDetailWindow(Unit unit) {
    super(unit.type.name);
    this.unit = unit;
  }

  @Override
  public void main(Table t) {
    if (unit == null) return;

    // 类型 meta
    t.add("[light]" + unit.type.name + "[]").left().pad(2f).row();
    t.add("[gray]类型描述等 meta（未来由 Entity 提供）[]").left().pad(2f).row();
    t.add().height(6f).row();

    // 属性：数字 + 条形图
    addStat(t, "血量", unit.health, unit.maxHealth, Color.scarlet);
    addStat(t, "护盾", unit.totalShield(), unit.totalShieldMax(), Color.sky);
    addStat(t, "护甲", unit.armor, unit.armorMax, Color.lightGray);
    addStat(t, "能量", unit.energy, unit.energyMax, Color.gold);
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

  private void addStat(Table t, String name, float cur, float max, Color color) {
    Table row = new Table();
    row.left();
    row.add("[gray]" + name + "[]").width(50f).left();
    row.add((int) cur + "/" + (int) max).width(90f).left();
    row.add(bar(cur, max, color)).size(120f, 8f).left();
    t.add(row).growX().left().row();
  }

  /** 条形图元素。 */
  private Element bar(float cur, float max, Color color) {
    return new Element() {
      {
        setSize(120f, 8f);
      }

      @Override
      public void draw() {
        float x = this.x;
        float y = this.y;
        float w = getWidth();
        float h = getHeight();

        Draw.color(Color.darkGray);
        Fill.rect(x + w / 2f, y + h / 2f, w, h);
        if (max > 0f) {
          float fw = w * Math.min(1f, cur / max);
          Draw.color(color);
          Fill.rect(x + fw / 2f, y + h / 2f, fw, h);
        }
        Draw.color();
      }
    };
  }
}
