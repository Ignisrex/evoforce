package com.silverignis.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.ScreenUtils;

/** Dev tool: draws one skill shader quad, animated, on a neutral background —
 *  for eyeballing a frag without driving into a battle.
 *  Run: gradlew shaderPreview [-Pshader=wind_slash] */
public final class ShaderPreview extends ApplicationAdapter {

    private final String id;
    /** Quad height / width — 1 for square effects, 0.24 for wind_slash's battle rect. */
    private final float ratio;
    private SpriteBatch batch;
    private ShaderProgram shader;
    private Texture pixel;
    private float time;

    private ShaderPreview(String id, float ratio) { this.id = id; this.ratio = ratio; }

    @Override
    public void create() {
        batch = new SpriteBatch();
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        shader = new ShaderProgram(
            Gdx.files.internal("ui/shaders/sprite_batch.vert"),
            Gdx.files.internal("skills/shaders/" + id + ".frag"));
        if (!shader.isCompiled()) throw new IllegalStateException(shader.getLog());
    }

    @Override
    public void render() {
        time += Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0.16f, 0.15f, 0.19f, 1f);   // roughly the cave floor's value
        // Largest quad of the requested aspect that fits the window.
        float w = Math.min(Gdx.graphics.getWidth() * 0.9f, Gdx.graphics.getHeight() * 0.9f / ratio);
        float h = w * ratio;
        float cx = Gdx.graphics.getWidth() * 0.5f, cy = Gdx.graphics.getHeight() * 0.5f;
        batch.begin();
        batch.setShader(shader);
        shader.setUniformf("u_time", time);
        shader.setUniformf("u_dir", 1f);
        shader.setUniformf("u_tint", 1f, 1f, 1f);
        if (shader.hasUniform("u_alpha")) shader.setUniformf("u_alpha", 1f);
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.draw(pixel, cx - w * 0.5f, cy - h * 0.5f, w, h);
        batch.setShader(null);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shader.dispose();
        pixel.dispose();
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setWindowedMode(800, 500);
        c.setTitle("shader-preview");
        String id    = args.length > 0 ? args[0] : "wind_slash";
        float  ratio = args.length > 1 ? Float.parseFloat(args[1]) : 1f;
        new Lwjgl3Application(new ShaderPreview(id, ratio), c);
    }
}
