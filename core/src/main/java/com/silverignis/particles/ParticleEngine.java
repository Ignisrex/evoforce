package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.silverignis.render.RenderContext;

/** Manager: owns the shared pool, the live emitters, and the draw routine.
 *  Emitters own their particle lists + sort depth and delegate drawing back here. */
public final class ParticleEngine {

    private final Vector2 scratch = new Vector2();   // reused across every emitter's draw this frame
    private final Color   tmp     = new Color();

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

    void draw(Array<Particle> live, EmitterSpec spec, RenderContext rc) {
        SpriteBatch batch = rc.batch;
        // additive = glowing energy; alpha = soft translucent puffs (mist/smoke)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, spec.additive ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < live.size; i++) {
            Particle p = live.get(i);
            float t = p.age / p.life;
            rc.project(p.pos.x, p.pos.z, scratch);
            float ds = rc.depthScale(p.pos.z);
            float size = p.sizeInterp.apply(p.sizeFrom, p.sizeFrom * p.sizeEndScale, t) * ds;
            float sy = scratch.y + p.pos.y * ds;
            tmp.set(p.colorFrom).lerp(p.colorTo, p.colorInterp.apply(t));
            batch.setColor(tmp.r, tmp.g, tmp.b, (1f - t) * tmp.a);   // color alpha caps peak opacity
            int n = spec.textures.length;
            Texture tx = spec.texturesOverLife
                ? spec.textures[Math.min((int) (t * n), n - 1)]   // ordered cycle over life
                : spec.textures[p.texIndex];                      // random pick made at spawn
            batch.draw(tx, scratch.x - size * 0.5f, sy - size * 0.5f, size, size);
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
