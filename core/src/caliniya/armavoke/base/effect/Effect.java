package caliniya.armavoke.base.effect;

import arc.func.Cons;
import arc.graphics.Color;

/** Immutable description of a reusable visual effect. */
public class Effect {
  public final float lifetime;
  public final float clip;
  public final float layer;
  public final Cons<EffectContainer> renderer;

  public Effect(float lifetime, Cons<EffectContainer> renderer) {
    this(lifetime, 64f, 0f, renderer);
  }

  public Effect(float lifetime, float clip, Cons<EffectContainer> renderer) {
    this(lifetime, clip, 0f, renderer);
  }

  public Effect(float lifetime, float clip, float layer, Cons<EffectContainer> renderer) {
    if (renderer == null) throw new IllegalArgumentException("Effect renderer cannot be null");
    this.lifetime = Math.max(0.001f, lifetime);
    this.clip = Math.max(0f, clip);
    this.layer = layer;
    this.renderer = renderer;
  }

  public void at(float x, float y) {
    at(x, y, 0f, Color.white, null);
  }

  public void at(float x, float y, Object data) {
    at(x, y, 0f, Color.white, data);
  }

  public void at(float x, float y, float rotation, Object data) {
    at(x, y, rotation, Color.white, data);
  }

  public void at(float x, float y, float rotation, Color color) {
    at(x, y, rotation, color, null);
  }

  public void at(float x, float y, float rotation, Color color, Object data) {
    Effects.emit(this, x, y, rotation, color, data);
  }
}
