package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Full-screen procedural aurora night sky (ui/shaders/aurora.frag) drawn as a
 * single quad through the batch. Palette lives here as public fields so a
 * screen can retint it before drawing.
 */
public final class AuroraBackground implements Disposable {
    public final Color skyTop = new Color(0.02f, 0.03f, 0.10f, 1f);
    public final Color skyBottom = new Color(0.08f, 0.04f, 0.16f, 1f);
    public final Color auroraA = new Color(0.20f, 0.95f, 0.75f, 1f);
    public final Color auroraB = new Color(0.70f, 0.35f, 1.00f, 1f);
    public final Color nebula = new Color(0.30f, 0.15f, 0.55f, 1f);
    public float intensity = 0.9f;

    private final ShaderProgram program;
    private final Texture pixel;
    private float time;

    public AuroraBackground(Texture pixel) {
        this.pixel = pixel;
        program = new ShaderProgram(
                Gdx.files.internal("ui/shaders/rounded_rect.vert"),
                Gdx.files.internal("ui/shaders/aurora.frag"));
        if (!program.isCompiled()) {
            throw new IllegalStateException("aurora shader failed to compile:\n" + program.getLog());
        }
    }

    public void update(float delta) {
        time += delta;
    }

    /** Draws a w×h quad from the origin; call between batch.begin()/end(). */
    public void draw(Batch batch, float w, float h) {
        batch.setShader(program);
        f("u_time", time);
        program.setUniformf("u_aspect", w / h);
        rgb("u_skyTop", skyTop);
        rgb("u_skyBottom", skyBottom);
        rgb("u_auroraA", auroraA);
        rgb("u_auroraB", auroraB);
        rgb("u_nebula", nebula);
        f("u_intensity", intensity);
        batch.draw(pixel, 0f, 0f, w, h);
        batch.setShader(null);
    }

    // Uniforms unused by the current shader source get stripped by the GLSL
    // compiler, so tolerate their absence (parts of the sky get toggled off
    // while iterating on the look).
    private void f(String uniform, float v) {
        if (program.hasUniform(uniform)) program.setUniformf(uniform, v);
    }

    private void rgb(String uniform, Color c) {
        if (program.hasUniform(uniform)) program.setUniformf(uniform, c.r, c.g, c.b);
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
