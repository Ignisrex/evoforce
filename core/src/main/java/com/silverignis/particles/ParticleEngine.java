package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;
import com.silverignis.render.SceneRenderable;

public final class ParticleEngine implements SceneRenderable {

    private final Texture tex;
    private final Vector2 scratch = new Vector2();
    public float depth;

    public ParticleEngine(Texture tex) { this.tex = tex; }

    private final Pool<Particle> pool = new Pool<Particle>() {
        @Override
        protected Particle newObject() {
            return new Particle();
        }
    };

    private final Array<Particle> live = new Array<>(false, 256);

    public Particle spawn(float x, float y, float z, float vx, float vy, float vz, float life) {
        Particle p = pool.obtain();
        p.pos.set(x, y, z);
        p.vel.set(vx, vy, vz);
        p.age = 0f;
        p.life = life;
        live.add(p);
        return p;
    }

    public void update(float dt) {
        for (int i = live.size - 1; i >= 0; i--) {
            Particle p = live.get(i);
            p.age += dt;
            if (p.age >= p.life){
                pool.free(live.removeIndex(i)); // reset() runs here; object goes back to pool
            } else {
                p.pos.mulAdd(p.vel, dt);      // pos += vel * deltaTime
            }
        }
    }

    public int liveCount() { return live.size; }

    public float depth() { return depth; }
    public RenderLayer layer() { return RenderLayer.BILLBOARD; }
    public void render(RenderContext rc) {
        SpriteBatch batch = rc.batch;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE); //additive energy glows

        for (int i = 0; i < live.size; i++ ){
            Particle p = live.get(i);
            rc.project(p.pos.x, p.pos.z, scratch);
            float depthScale = rc.depthScale(p.pos.z);
            float size = p.size * depthScale;
            float sy = scratch.y + p.pos.y * depthScale;   //fake height: world Y -> screen, depth-scaled
            float fade = 1f - p.age / p.life;               //ponytail: linear; real curves in M4
            batch.setColor(p.color.r, p.color.g, p.color.b, p.color.a * fade);
            batch.draw(tex, scratch.x  - size * 0.5f, sy - size * 0.5f, size, size);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); //restore normal alpha
    }
}
