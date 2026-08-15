package caliniya.armavoke.ui.windows;

import arc.Core;
import arc.func.Floatf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.core.meta.stat.StatStack;
import caliniya.armavoke.type.ability.Ability;
import caliniya.armavoke.type.Enhancement;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.game.Contents;
import caliniya.armavoke.base.type.CType;
import caliniya.armavoke.type.type.ItemType;
import caliniya.armavoke.type.type.LiquidType;

/** 单位详细信息窗口： 类型信息（名字 + DataWindow 按钮 + stats meta）、 血量/能量/护盾（数字 + 条形图，实时更新）、能力列表（可开关能力带开关按钮）。 */
public class UnitDetailWindow extends Window {

  private final Unit unit;

  public StatStack stst;

  public UnitDetailWindow(Unit unit) {
    super(unit.type.localizedName);
    this.unit = unit;
    this.stst = new StatStack();
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
        .add(
            new Button(
                Core.bundle.get("unitDetail.info"), () -> new DataWindow(unit.type.stat).build()))
        .size(84f, 36f)
        .padLeft(8f);
    t.add(nameRow).growX().left().row();
    
    
    
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
