package com.silverignis.render;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.entities.Battlefield;
import com.silverignis.environment.GameEnvironment;

/**
 * What a {@link SceneRenderable} is allowed to reach during a draw: the batch,
 * the font, and the tile → screen mapping. Deliberately offers no route back
 * into the simulation.
 *
 * This is where the old BattleContext projection cache went. It is not cached
 * any more — a projection is a matrix multiply and there are a few dozen a
 * frame, so baking them bought nothing and cost the resize() coupling that kept
 * combat from being constructible without a viewport.
 */
public final class RenderContext {

    public final SpriteBatch batch;
    public final BitmapFont font;

    private final GameEnvironment env;

    private final Vector2 scratch = new Vector2();

    public RenderContext(SpriteBatch batch, BitmapFont font, GameEnvironment gameEnvironment) {
        this.batch = batch;
        this.font = font;
        this.env = gameEnvironment;
    }

    /** Identity projection for flat UI stages: (x, z) pass through as screen coords. */
    public static RenderContext screenSpace(SpriteBatch batch) {
        return new RenderContext(batch, null, null);
    }

    public Vector2 project(float worldX, float worldZ) { return env.project(worldX, worldZ); }
    public Vector2 project(float worldX, float worldZ, Vector2 out) {
        return env == null ? out.set(worldX, worldZ) : env.project(worldX, worldZ, out);
    }
    public float depthScale(float worldZ){ return env == null ? 1f : env.depthScale(worldZ); }

    // ── tile space ────────────────────────────────────────────────────────
    // Columns and rows are continuous so a projectile or a mid-step entity can
    // sit between tiles. The returned Vector2 is reused — read it before the
    // next call.

    /** Shared scratch — read it before the next call. Use the {@code out}
     *  overload when you need two tile positions live at once. */
    public Vector2 tileWorld(float col, float row) {
        return tileWorld(col, row, scratch);
    }

    public Vector2 tileWorld(float col, float row, Vector2 out) {
        return project(Battlefield.floorX(col), Battlefield.floorZ(row), out);
    }

    public float tileDepthScale(float row) { return depthScale(Battlefield.floorZ(row)); }

    /** Panel draw sizes, forwarded so a renderable needs only this one object. */
    public float panelWidth()        { return Battlefield.getPanelWidth(); }
    public float panelHeight()       { return Battlefield.getPanelHeight(); }
    public float panelRenderHeight() { return Battlefield.getPanelRenderHeight(); }
}
