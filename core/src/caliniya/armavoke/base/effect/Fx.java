package caliniya.armavoke.base.effect;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;

/** Built-in effects used by core combat and production events. */
public final class Fx {
  public static final Effect hit =
      new Effect(
          14f,
          32f,
          effect -> {
            Draw.color(effect.color, effect.fout());
            Fill.circle(effect.x, effect.y, 1.5f + 2f * effect.fout());
            Lines.stroke(0.5f + 1.5f * effect.fout());
            Lines.circle(effect.x, effect.y, 2f + 10f * effect.fin());
          });

  public static final Effect spawn =
      new Effect(
          30f,
          56f,
          effect -> {
            Draw.color(Color.sky, effect.fout());
            Lines.stroke(0.5f + effect.fout() * 2f);
            Lines.circle(effect.x, effect.y, 6f + effect.fin() * 24f);
            Fill.square(effect.x, effect.y, 2f + effect.fslope() * 3f, 45f);
          });

  public static final Effect destroy =
      new Effect(
          36f,
          72f,
          effect -> {
            float length = 8f + effect.fin() * 30f;
            Draw.color(Color.scarlet, effect.fout());
            Lines.stroke(0.5f + effect.fout() * 2f);
            Lines.circle(effect.x, effect.y, 4f + effect.fin() * 22f);
            for (int i = 0; i < 6; i++) {
              float angle = effect.rotation + i * 60f;
              Lines.line(
                  effect.x,
                  effect.y,
                  effect.x + Mathf.cosDeg(angle) * length,
                  effect.y + Mathf.sinDeg(angle) * length);
            }
          });

  private Fx() {}
}
