package caliniya.armavoke.base.effect;

import arc.graphics.Color;

/** Runtime values supplied to an {@link Effect} renderer. */
public class EffectContainer {
  public int id;
  public float x;
  public float y;
  public float rotation;
  public float time;
  public float lifetime;
  public final Color color = new Color();
  public Object data;
  public Effect effect;

  EffectContainer set(
      int id, Effect effect, float x, float y, float rotation, Color color, Object data) {
    this.id = id;
    this.effect = effect;
    this.x = x;
    this.y = y;
    this.rotation = rotation;
    this.time = 0f;
    this.lifetime = effect.lifetime;
    this.color.set(color == null ? Color.white : color);
    this.data = data;
    return this;
  }

  public float fin() {
    return Math.max(0f, Math.min(1f, time / lifetime));
  }

  public float fout() {
    return 1f - fin();
  }

  public float fslope() {
    return 1f - Math.abs(fin() * 2f - 1f);
  }

  @SuppressWarnings("unchecked")
  public <T> T data() {
    return (T) data;
  }
}
