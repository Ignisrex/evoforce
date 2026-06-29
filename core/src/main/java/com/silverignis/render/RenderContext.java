package com.silverignis.render;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.environment.GameEnvironment;

public final class RenderContext {

    public final SpriteBatch batch;
    private final GameEnvironment env;
    public final BitmapFont font;

    public RenderContext(SpriteBatch batch, BitmapFont font, GameEnvironment gameEnvironment){
        this.batch = batch;
        this.env = gameEnvironment;
        this.font = font;
    }

    public Vector2 project(float worldX, float worldZ) { return env.project(worldX, worldZ); }
    public Vector2 project(float worldX, float worldZ, Vector2 out) { return env.project(worldX, worldZ, out); }
    public float depthScale(float worldZ){ return env.depthScale(worldZ); }
}
