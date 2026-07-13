package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Draws a rounded rect (fill and/or border ring) as a single quad whose
 * corners are carved by a signed-distance function in the fragment shader
 * (ui/shaders/rounded_rect.*) — crisp at any scale, radius and thickness as
 * uniforms, no corner textures to bake. Opacity comes from the batch color,
 * so actor/parent fades pass through untouched.
 *
 * Each call swaps the batch shader in and out (two flushes). Fine for a
 * paused menu's worth of rects.
 * ponytail: per-draw uniforms; pack params into vertex attributes if this
 * ever draws hundreds of rects per frame.
 */
public final class RoundedRectShader implements Disposable {
    private static final Color TRANSPARENT = new Color(0f, 0f, 0f, 0f);

    private final ShaderProgram program;
    private final Texture pixel;

    public RoundedRectShader(Texture pixel) {
        this.pixel = pixel;
        program = new ShaderProgram(
                Gdx.files.internal("ui/shaders/rounded_rect.vert"),
                Gdx.files.internal("ui/shaders/rounded_rect.frag"));
        if (!program.isCompiled()) {
            throw new IllegalStateException("rounded_rect shader failed to compile:\n" + program.getLog());
        }
    }

    /**
     * Any color may be null to skip that part; radius 0 gives square corners.
     * A glow inflates the emitted quad by {@code glowWidth} so the neon spill
     * lands outside the rect's own bounds.
     */
    void draw(Batch batch, float x, float y, float w, float h,
              Color fill, Color border, float borderWidth, float radius,
              Color glow, float glowWidth) {
        if (fill == null && border == null) return;
        float spill = glow == null ? 0f : glowWidth;
        batch.setShader(program); // flushes pending default-shader quads, binds program
        program.setUniformf("u_halfSize", w / 2f, h / 2f);
        program.setUniformf("u_radius", radius);
        program.setUniformf("u_borderWidth", border == null ? 0f : borderWidth);
        program.setUniformf("u_glowWidth", spill);
        color("u_fillColor", fill == null ? TRANSPARENT : fill);
        color("u_borderColor", border == null ? fill : border);
        color("u_glowColor", glow == null ? TRANSPARENT : glow);
        batch.draw(pixel, x - spill, y - spill, w + 2 * spill, h + 2 * spill);
        batch.setShader(null);    // flushes our quad while these uniforms apply
    }

    private void color(String uniform, Color c) {
        program.setUniformf(uniform, c.r, c.g, c.b, c.a);
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
