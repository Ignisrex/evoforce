package com.silverignis.particles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;
import com.silverignis.render.SceneRenderable;

public final class Emitter implements SceneRenderable {

    private final EmitterSpec spec;
    private final Anchor anchor;
    private final Drive drive;

    private final Array<Particle> live = new Array<>(false, 64);
    private final Vector3 scratch = new Vector3();

    private ParticleEngine engine;   // set on add(); owns the pool + the draw routine

    private float accumulator;
    private int emitted;
    private boolean stopped;

    private float depth;
    private final Channel channel;

    public Emitter(EmitterSpec spec, Anchor anchor, Drive drive, Channel channel) {
        this.anchor = anchor;
        this.spec = spec;
        this.drive = drive;
        this.channel = channel;
        anchor.point(scratch);
        this.depth = scratch.z;
    }

    void bind(ParticleEngine engine) { this.engine = engine; }

    public void stop() { stopped = true; }

    public boolean update(float dt) {
        if (!stopped) spawn(dt);
        for(int i = live.size - 1; i>= 0; i--){
            Particle p = live.get(i);
            p.age += dt;
            if(p.age >= p.life) engine.free(live.removeIndex(i));
            else p.pos.mulAdd(p.vel, dt);
        }
        boolean doneSpawning = stopped || (spec.mode == EmitterSpec.Mode.BURST && emitted >= spec.count);
        return  doneSpawning && live.size == 0;
    }

    private void spawn(float dt) {
        int n;
        if(spec.mode == EmitterSpec.Mode.CONTINUOUS) {
            accumulator += spec.rate * drive.value() * dt;
            n = (int) accumulator;
            accumulator -= n;
        }else if (spec.window <= 0f) {
            n = spec.count - emitted;
            emitted = spec.count;
        }else {
            accumulator += spec.count * (dt / spec.window);
            n = Math.min((int) accumulator, spec.count -emitted);
            accumulator -= n;
            emitted += n;
        }
        for (int i = 0; i< n; i++) spawnOne();
    }

    public void spawnOne() {
        anchor.point(scratch);
        float sp = spec.speed.sample();
        float polar = MathUtils.random(-spec.spreadDeg, spec.spreadDeg) * MathUtils.degRad; //tilt off +Y
        float az = MathUtils.random(0f, MathUtils.PI2);
        float horiz = sp * MathUtils.sin(polar);
        Particle p = engine.obtain();
        p.pos.set(scratch);
        p.vel.set(
            horiz * MathUtils.cos(az) + spec.driftX,
            sp * MathUtils.cos(polar) + spec.driftY,
            horiz * MathUtils.sin(az) + spec.driftZ);

        p.age = 0f;
        p.life = spec.life.sample();

        p.sizeFrom = spec.size.sample();
        p.sizeEndScale = spec.sizeEndScale;
        p.sizeInterp = spec.sizeInterp;


        p.colorFrom.set(spec.colorFrom);
        p.colorTo.set(spec.colorTo);
        p.colorInterp = spec.colorInterp;

        live.add(p);
    }

    public int liveCount() { return live.size; }
    public float depth() { return depth; }
    public Channel channel() { return channel; }

    void clear() {
        for (Particle p : live) engine.free(p);
        live.clear();
    }

    @Override
    public RenderLayer layer() {
        return RenderLayer.BILLBOARD;
    }

    public void render(RenderContext rc) { engine.draw(live, spec.texture, rc); }
}
