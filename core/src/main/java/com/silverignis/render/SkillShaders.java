package com.silverignis.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * Procedural in-flight skill visuals (skills/shaders/*.frag), each drawn as a
 * single quad over the shared 1x1 white pixel so the fragment shader gets
 * clean 0..1 UVs. A skill opts in via its {@code "shader"} field in
 * skills.json; the id must be listed here so a typo fails at boot, not
 * mid-battle.
 *
 * Each draw swaps the batch shader and blend func and restores both — the
 * same discipline as ParticleEngine.draw — so it can sit anywhere in the
 * sorted billboard bucket. Costs two extra flushes per projectile.
 */
public final class SkillShaders implements Disposable {

    private static final String[] IDS = { "dark_blast", "wind_slash", "shield", "frost_trap" };

    private final Map<String, ShaderProgram> programs = new HashMap<>();
    private final Texture pixel;

    public SkillShaders(Texture pixel) {
        this.pixel = pixel;
        for (String id : IDS) {
            ShaderProgram p = new ShaderProgram(
                    Gdx.files.internal("ui/shaders/sprite_batch.vert"),
                    Gdx.files.internal("skills/shaders/" + id + ".frag"));
            if (!p.isCompiled()) {
                throw new IllegalStateException("skill shader '" + id + "' failed to compile:\n" + p.getLog());
            }
            programs.put(id, p);
        }
    }

    public boolean has(String id) { return programs.containsKey(id); }

    /** Draws one shader quad of side {@code size} centered on (cx, cy). */
    public void draw(SpriteBatch batch, String id, float cx, float cy, float size,
                     float time, float dir, Color tint) {
        draw(batch, id, cx, cy, size, size, time, dir, tint);
    }

    /** Rectangular variant — for effects that must match a sprite's aspect. */
    public void draw(SpriteBatch batch, String id, float cx, float cy, float w, float h,
                     float time, float dir, Color tint) {
        ShaderProgram p = programs.get(id);
        batch.setShader(p);  // flushes pending default-shader quads, binds p
        p.setUniformf("u_time", time);
        if (p.hasUniform("u_dir")) p.setUniformf("u_dir", dir);
        p.setUniformf("u_tint", tint.r, tint.g, tint.b);
        // Optional envelope: shaders that fade with their phase declare u_alpha
        // and get the tint's alpha; the rest ignore it.
        if (p.hasUniform("u_alpha")) p.setUniformf("u_alpha", tint.a);
        // Premultiplied alpha: rgb carries both lit color and additive glow,
        // alpha carries occlusion, so one draw can darken and add light.
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.draw(pixel, cx - w * 0.5f, cy - h * 0.5f, w, h);
        batch.setShader(null);  // flushes our quad while these uniforms apply
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void dispose() {
        for (ShaderProgram p : programs.values()) p.dispose();
    }
}
