package caliniya.armavoke.ui.windows;

import arc.Core;
import arc.func.Floatf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Align;
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
    main =
        new Table() {
          @Override
          public void draw() {
            main(this);
            super.draw();
          };
        };
  }

  @Override
  public void main(Table t) {
    if (unit == null) return;
    t.clear();
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

    // 组装无分组运行时数据：实体（血量/护甲/护盾/能量/热量/电力）+ 能力 + 模组
    unit.stat(stst);
    for (Enhancement enh : unit.enhancements) {
      enh.type.stats(stst);
    }

    // 渲染：完整遍历所有 StatData（data 已含缩进），跳过空内容
    stst.each(
        d -> {
          if (d.data == null || d.data.trim().isEmpty()) return;
          t.add(d.data).left().padBottom(2).align(Align.left);
          t.row();
        });

    // 物品数据显示区（TODO：由开发者补充，展示 unit.item / unit.liquid 各资源量）
    // TODO 物品数据

    // 可开关模组 + 能力（带开关按钮）
    t.add().height(8f).row();
    for (Enhancement enh : unit.enhancements) {
      Table row = new Table();
      row.left();
      row.add("[gray]" + enh.type.localizedName + "[]").left().pad(2f);
      row.add(
              new Button(
                  enh.enabled
                      ? Core.bundle.get("unitDetail.disable")
                      : Core.bundle.get("unitDetail.enable"),
                  () -> {
                    enh.setEnabled(!enh.enabled);
                    main(this.main); // 刷新窗口内容
                  }))
          .size(64f, 36f)
          .padLeft(6f);
      t.add(row).growX().left().row();
    }
    for (Ability a : unit.abilities) {
      if (!a.toggleable) continue;
      Table row = new Table();
      row.left();
      row.add("[gray]" + a.localizedName + "[]").left().pad(2f);
      row.add(
              new Button(
                  a.enabled
                      ? Core.bundle.get("unitDetail.disable")
                      : Core.bundle.get("unitDetail.enable"),
                  () -> {
                    a.setEnabled(!a.enabled);
                    main(this.main); // 刷新窗口内容
                  }))
          .size(64f, 36f)
          .padLeft(6f);
      t.add(row).growX().left().row();
    }
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
