package com.silverignis.skills.visuals;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.silverignis.particles.*;

public abstract class AbstractSkillVisual implements SkillVisual {

    private final Array<EmitterHandle> handles = new Array<>(false, 4);
    private boolean ended = false;

    @Override
    public final void onTrigger(Trigger t, VisualState state, ParticleEngine engine) {
        if (t == Trigger.END) {
            ended = true;
            stopEmitters();
        }
        react(t, state, engine);
    }

    protected abstract void react(Trigger t, VisualState state, ParticleEngine engine);

    @Override
    public void update(float delta) {}

    @Override
    public boolean isDone() { return ended; }

    protected final boolean hasEnded() {return ended; }

    protected final void play(ParticleEngine engine, EffectDef effect, Anchor anchor, Drive drive){
        handles.add(effect.play(engine, anchor, drive, Channel.COMBAT));
    }

    protected final void play(ParticleEngine engine, EffectDef effect, Anchor anchor) {
        play(engine, effect, anchor, Drive.FULL);
    }

    protected final void burst(ParticleEngine engine, EffectDef effect, Vector3 pos) {
        effect.play(engine, Anchor.at(pos.x, pos.y, pos.z), Channel.COMBAT);
    }

    protected static Anchor track(Vector3 live) {
        return out -> out.set(live);
    }

    protected final void stopEmitters() {
        for (EmitterHandle h : handles) if (h != null) h.stop();
        handles.clear();
    }
}
