package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * The main menu scene: the painted {@code mainmenu_background.png} with its
 * living regions (water, dust, frost, wind) animated in place by
 * ui/shaders/menu_backdrop.frag, plus the lightning drawn from scratch, so
 * the painting ships without it.
 */
public final class MenuBackdrop implements Disposable {
    /** Master multiplier on every mover; 0 leaves the still painting. */
    public float intensity = 1f;

    private final ShaderProgram program;
    private final Texture painting;
    private float time;

    public MenuBackdrop() {
        painting = new Texture(Gdx.files.internal("mainmenu_background.png"), true);   // mipmapped: the shader blurs through them
        painting.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        program = new ShaderProgram(
                Gdx.files.internal("ui/shaders/sprite_batch.vert"),
                Gdx.files.internal("ui/shaders/menu_backdrop.frag"));
        if (!program.isCompiled()) {
            throw new IllegalStateException("menu backdrop shader failed to compile:\n" + program.getLog());
        }
    }

    public void update(float delta) {
        time += delta;
    }

    /** Fills a w×h frame from the origin; call between batch.begin()/end(). */
    public void draw(Batch batch, float w, float h) {
        batch.setShader(program);
        program.setUniformf("u_time", time);
        program.setUniformf("u_aspect", w / h);
        program.setUniformf("u_intensity", intensity);
        batch.draw(painting, 0f, 0f, w, h);
        batch.setShader(null);
    }

    @Override
    public void dispose() {
        program.dispose();
        painting.dispose();
    }
}
