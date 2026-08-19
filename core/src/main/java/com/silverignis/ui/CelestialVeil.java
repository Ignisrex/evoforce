package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Full-screen procedural night sky (ui/shaders/celestial_veil.frag) drawn as a
 * single quad through the batch: a starfield and nebula haze under aurora curtains
 * hanging from above, plus a radiance behind screen centre to seat the cards.
 * Palette lives here as public fields so a screen can retint it before drawing.
 */
public final class CelestialVeil implements Disposable {
    public final Color skyTop = new Color(0.02f, 0.03f, 0.10f, 1f);
    public final Color skyBottom = new Color(0.08f, 0.04f, 0.16f, 1f);
    public final Color nebula = new Color(0.30f, 0.15f, 0.55f, 1f);
    public final Color veilA = new Color(0.25f, 0.90f, 0.80f, 1f);
    public final Color veilB = new Color(0.62f, 0.38f, 1.00f, 1f);
    public final Color radiance = new Color(0.28f, 0.20f, 0.48f, 1f);
    public float intensity = 0.95f;

    private final ShaderProgram program;
    private final Texture pixel;
    private float time;

    public CelestialVeil(Texture pixel) {
        this.pixel = pixel;
        program = new ShaderProgram(
                Gdx.files.internal("ui/shaders/sprite_batch.vert"),
                Gdx.files.internal("ui/shaders/celestial_veil.frag"));
        if (!program.isCompiled()) {
            throw new IllegalStateException("celestial veil shader failed to compile:\n" + program.getLog());
        }
    }

    public void update(float delta) {
        time += delta;
    }

    /** Draws a w×h quad from the origin; call between batch.begin()/end(). */
    public void draw(Batch batch, float w, float h) {
        batch.setShader(program);
        program.setUniformf("u_time", time);
        program.setUniformf("u_aspect", w / h);
        rgb("u_skyTop", skyTop);
        rgb("u_skyBottom", skyBottom);
        rgb("u_nebula", nebula);
        rgb("u_veilA", veilA);
        rgb("u_veilB", veilB);
        rgb("u_radiance", radiance);
        program.setUniformf("u_intensity", intensity);
        batch.draw(pixel, 0f, 0f, w, h);
        batch.setShader(null);
    }

    private void rgb(String uniform, Color c) {
        program.setUniformf(uniform, c.r, c.g, c.b);
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
