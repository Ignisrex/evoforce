package com.silverignis.particles;

import com.badlogic.gdx.utils.Array;

import java.util.function.Consumer;


public class EffectDef {

    private final EmitterSpec[] specs;

    private EffectDef(EmitterSpec[] specs) { this.specs = specs;}

    public static Builder effect(){ return new Builder(); }

    public EmitterHandle play(ParticleEngine engine, Anchor anchor, Drive drive, Channel channel) {
        Emitter[] made = new Emitter[specs.length];
        for(int i = 0; i < specs.length; i++){
            made[i] = new Emitter(specs[i], anchor, drive, channel);
            engine.add(made[i]);
        }
        return () -> { for (Emitter e : made) e.stop(); };
    }

    public void play(ParticleEngine engine, Anchor anchor, Channel channel) { play(engine, anchor, Drive.FULL, channel); }

    public static final class Builder {
        private final Array<EmitterSpec> specs = new Array<>(false, 4);

        public Builder emitter(Consumer<EmitterSpec.Builder> cfg) {
            EmitterSpec.Builder b = new EmitterSpec.Builder();
            cfg.accept(b);
            specs.add(b.build());
            return this;
        }

        public EffectDef build() { return new EffectDef(specs.toArray(EmitterSpec.class));}
    }
}
