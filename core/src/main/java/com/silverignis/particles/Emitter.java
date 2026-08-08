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
    private final Vector3 lastAnchor = new Vector3();
    private boolean hasLastAnchor;

    private ParticleEngine engine;   // set on add(); owns the pool + the draw routine

    private float accumulator;
    private float delayLeft;
    private int emitted;
    private int spawned;
    private boolean stopped;

    private float depth;
    private final Channel channel;

    public Emitter(EmitterSpec spec, Anchor anchor, Drive drive, Channel channel) {
        this.anchor = anchor;
        this.spec = spec;
        this.drive = drive;
        this.channel = channel;
        this.delayLeft = spec.delay;
        anchor.point(scratch);
        this.depth = scratch.z;
    }

    void bind(ParticleEngine engine) { this.engine = engine; }

    public void stop() { stopped = true; }

    public boolean update(float dt) {
        if (delayLeft > 0f) delayLeft -= dt;
        else if (!stopped) spawn(dt);
        for(int i = live.size - 1; i>= 0; i--){
            Particle p = live.get(i);
            p.age += dt;
            if(p.age >= p.life) engine.free(live.removeIndex(i));
            else {
                p.vel.add(spec.accelX * dt, spec.accelY * dt, spec.accelZ * dt);
                if (spec.wander > 0f) {
                    float w = spec.wander * dt;   // ponytail: dt-scaled random walk, eyeball-tuned, not framerate-exact
                    p.vel.add(MathUtils.random(-w, w), MathUtils.random(-w, w), MathUtils.random(-w, w));
                }
                if (spec.drag > 0f) p.vel.scl(Math.max(0f, 1f - spec.drag * dt));
                p.pos.mulAdd(p.vel, dt);
                p.rot += p.spin * dt;
            }
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
        if (spec.follow) scratch.setZero();   // follow: pos is an offset; anchor re-added at draw
        else anchor.point(scratch);
        // inherit: sampled off the raw anchor point, before offset/jitter muddy the path direction
        float ivx = 0f, ivy = 0f, ivz = 0f;
        if (spec.inherit != 0f && !spec.follow) {
            if (hasLastAnchor) {
                ivx = (scratch.x - lastAnchor.x) * spec.inherit;
                ivy = (scratch.y - lastAnchor.y) * spec.inherit;
                ivz = (scratch.z - lastAnchor.z) * spec.inherit;
            }
            lastAnchor.set(scratch);
            hasLastAnchor = true;
        }
        scratch.add(spec.offsetX, spec.offsetY, spec.offsetZ);
        if (spec.jitterX != 0f || spec.jitterY != 0f || spec.jitterZ != 0f) {
            scratch.add(
                MathUtils.random(-spec.jitterX, spec.jitterX),
                MathUtils.random(-spec.jitterY, spec.jitterY),
                MathUtils.random(-spec.jitterZ, spec.jitterZ));
        }
        float sp = spec.speed.sample();
        float polar = MathUtils.random(-spec.spreadDeg, spec.spreadDeg) * MathUtils.degRad; //tilt off +Y
        float az = MathUtils.random(0f, MathUtils.PI2);
        float horiz = sp * MathUtils.sin(polar);
        Particle p = engine.obtain();
        p.pos.set(scratch);
        p.vel.set(
            horiz * MathUtils.cos(az),
            sp * MathUtils.cos(polar),
            horiz * MathUtils.sin(az));
        if (spec.dirRot != null) spec.dirRot.transform(p.vel);
        p.vel.add(spec.driftX + ivx, spec.driftY + ivy, spec.driftZ + ivz);

        p.age = 0f;
        p.life = spec.life.sample();
        p.rot = spec.rotation.sample();
        p.spin = spec.spin.sample();
        p.texIndex = (!spec.texturesOverLife && spec.textures.length > 1)
            ? MathUtils.random(spec.textures.length - 1) : 0;   // over-life mode indexes by age instead

        float burstT = (spec.mode == EmitterSpec.Mode.BURST && spec.count > 1)
            ? Math.min(spawned / (spec.count - 1f), 1f) : 1f;
        spawned++;
        p.sizeFrom = spec.size.sample()
            * (spec.sizeBurstStart + (1f - spec.sizeBurstStart) * burstT);
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

    public void render(RenderContext rc) {
        if (spec.follow) anchor.point(scratch);
        else scratch.setZero();
        engine.draw(live, spec, scratch, rc);
    }
}
