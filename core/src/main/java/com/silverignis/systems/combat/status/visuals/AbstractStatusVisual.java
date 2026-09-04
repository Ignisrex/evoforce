package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.silverignis.particles.*;
import com.silverignis.systems.combat.Combatant;

/** Emitter bookkeeping shared by status visuals (mirrors AbstractSkillVisual). */
public abstract class AbstractStatusVisual implements StatusVisual {

    private final Array<EmitterHandle> handles = new Array<>(false, 2);
    private boolean ended;

    @Override public void begin(Combatant owner, ParticleEngine particles) {}

    @Override public void end() {
        ended = true;
        for (EmitterHandle h : handles) if (h != null) h.stop();
        handles.clear();
    }

    @Override public boolean isDone() { return ended; }

    protected final void play(ParticleEngine engine, EffectDef effect, Anchor anchor) {
        handles.add(effect.play(engine, anchor, Drive.FULL, Channel.COMBAT));
    }

    /** Color.lerp toward {@code target} by {@code k}, keeping the current alpha. */
    protected static void blend(Color inOut, Color target, float k) {
        float a = inOut.a;
        inOut.lerp(target, k);
        inOut.a = a;
    }
}
