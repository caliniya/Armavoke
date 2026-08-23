package caliniya.armavoke.base.effect;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import caliniya.armavoke.base.type.EventType;
import caliniya.armavoke.system.System;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** Main-thread effect renderer with a thread-safe event ingress queue. */
public class Effects extends System<Effects> {
  private static final int maxActive = 1024;
  private static final AtomicInteger ids = new AtomicInteger();
  private static final ConcurrentLinkedQueue<EffectContainer> pending =
      new ConcurrentLinkedQueue<>();
  private static final Comparator<EffectContainer> byLayer =
      (a, b) -> Float.compare(a.effect.layer, b.effect.layer);

  private final Seq<EffectContainer> active = new Seq<>(false, 128);

  static void emit(
      Effect effect, float x, float y, float rotation, Color color, Object data) {
    pending.add(
        new EffectContainer().set(ids.incrementAndGet(), effect, x, y, rotation, color, data));
  }

  @Override
  public Effects init() {
    index = 14;
    Events.run(EventType.events.Mapinit, this::clear);
    Events.run(
        EventType.events.EnterUV,
        () -> {
          paused = true;
          clear();
        });
    Events.run(EventType.events.ExitUV, () -> paused = false);
    return super.init(false, false);
  }

  @Override
  public void update() {
    if (!inited || paused) return;

    drainPending();
    float delta = Math.min(Time.delta, 4f);
    for (int i = active.size - 1; i >= 0; i--) {
      EffectContainer state = active.get(i);
      if (state.time >= state.lifetime) {
        active.remove(i);
        continue;
      }

      if (visible(state)) {
        try {
          state.effect.renderer.get(state);
        } catch (Throwable error) {
          Log.err("Effect renderer failed", error);
          active.remove(i);
          continue;
        } finally {
          Draw.color();
          Lines.stroke(1f);
        }
      }
      state.time += delta;
    }
  }

  private void drainPending() {
    EffectContainer state;
    boolean changed = false;
    while ((state = pending.poll()) != null) {
      if (active.size >= maxActive) active.remove(0);
      active.add(state);
      changed = true;
    }
    if (changed && active.size > 1) active.sort(byLayer);
  }

  private boolean visible(EffectContainer state) {
    if (Core.camera == null) return true;
    float clip = state.effect.clip;
    return Math.abs(state.x - Core.camera.position.x) <= Core.camera.width / 2f + clip
        && Math.abs(state.y - Core.camera.position.y) <= Core.camera.height / 2f + clip;
  }

  public void clear() {
    pending.clear();
    active.clear();
  }
}
