package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.silverignis.render.RenderContext;

/** Manager: owns the shared pool, the live emitters, and the draw routine.
 *  Emitters own their particle lists + sort depth and delegate drawing back here. */
public final class ParticleEngine {

    private static final float STRETCH_EPS = 0.05f;   // seconds of travel used to sample the screen angle

    private final Vector2 scratch  = new Vector2();   // reused across every emitter's draw this frame
    private final Vector2 scratch2 = new Vector2();   // stretch: projected pos + vel·ε for the screen angle
    private final Color   tmp      = new Color();

    private final Pool<Particle> pool = new Pool<Particle>() {
        @Override protected Particle newObject() { return new Particle(); }
    };

    private final Array<Emitter> emitters = new Array<>(false, 16);

    public void add(Emitter e) { e.bind(this); emitters.add(e); }

    /** Submit these to the WorldRenderer — each emitter draws at its own depth. */
    public Array<Emitter> emitters() { return emitters; }

    // Pool access + draw for the emitters (package-private — this pairing is internal).
    Particle obtain()          { return pool.obtain(); }
    void     free(Particle p)  { pool.free(p); }

    /** base = the anchor's current point for follow emitters (particle pos is an offset), zero otherwise. */
    void draw(Array<Particle> live, EmitterSpec spec, Vector3 base, RenderContext rc) {
        SpriteBatch batch = rc.batch;
        // additive = glowing energy; alpha = soft translucent puffs (mist/smoke)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, spec.additive ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < live.size; i++) {
            Particle p = live.get(i);
            float t = p.age / p.life;
            float px = base.x + p.pos.x, py = base.y + p.pos.y, pz = base.z + p.pos.z;
            rc.project(px, pz, scratch);
            float ds = rc.depthScale(pz);
            float size = p.sizeInterp.apply(p.sizeFrom, p.sizeFrom * p.sizeEndScale, t) * ds;
            float sy = scratch.y + py * ds;

            float alpha = spec.alphaInterp.apply(1f - t);
            if (t < spec.fadeIn) alpha *= t / spec.fadeIn;
            tmp.set(p.colorFrom).lerp(p.colorTo, p.colorInterp.apply(t));
            batch.setColor(tmp.r, tmp.g, tmp.b, alpha * tmp.a);   // color alpha caps peak opacity

            float rot = p.rot;
            float w = size;
            if (spec.stretch > 0f) {
                // screen-space motion direction: project a point ε further along the velocity
                float ez = pz + p.vel.z * STRETCH_EPS;
                rc.project(px + p.vel.x * STRETCH_EPS, ez, scratch2);
                float dy = (scratch2.y + (py + p.vel.y * STRETCH_EPS) * rc.depthScale(ez)) - sy;
                rot = MathUtils.atan2(dy, scratch2.x - scratch.x) * MathUtils.radDeg;
                w = size * (1f + spec.stretch * p.vel.len());
            }

            int n = spec.textures.length;
            TextureRegion tx = spec.texturesOverLife
                ? spec.textures[Math.min((int) (t * n), n - 1)]   // ordered cycle over life
                : spec.textures[p.texIndex];                      // random pick made at spawn
            batch.draw(tx, scratch.x - w * 0.5f, sy - size * 0.5f,
                w * 0.5f, size * 0.5f, w, size, 1f, 1f, rot);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void update(float dt) {
        for (int i = emitters.size - 1; i >= 0; i--) {
            if (emitters.get(i).update(dt)) emitters.removeIndex(i);
        }
    }

    public int liveCount() {
        int n = 0;
        for (Emitter e : emitters) n += e.liveCount();
        return n;
    }

    public void clear(Channel channel) {
        for (int i = emitters.size - 1; i >= 0; i--) {
            Emitter e = emitters.get(i);
            if (e.channel() == channel) { e.clear(); emitters.removeIndex(i); }
        }
    }
}
