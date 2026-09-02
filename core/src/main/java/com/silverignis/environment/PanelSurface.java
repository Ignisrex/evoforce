package com.silverignis.environment;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Disposable;
import com.silverignis.entities.Battlefield;

/**
 * One elemental panel look. The {@link PanelSurfaces} host owns the pass
 * (GL state, tile diff, shared quad, common uniforms) and calls in:
 * {@code tileGained}/{@code tileLost} when a tile becomes / stops being this
 * type (initial layout included), {@code render} once per frame.
 */
public abstract class PanelSurface implements Disposable {

    protected PanelSurfaces host;
    protected final Battlefield.PanelType type;

    protected PanelSurface(Battlefield.PanelType type) { this.type = type; }

    final void bind(PanelSurfaces host) { this.host = host; }

    public final Battlefield.PanelType type() { return type; }

    /** A tile became this type. Build per-tile state (meshes, emitters) here. */
    public void tileGained(int col, int row) {}

    /** A tile stopped being this type. Tear down what tileGained built. */
    public void tileLost(int col, int row) {}

    /** Draw every tile of this type. Depth test on, blending off, culling off. */
    public abstract void render(Camera cam);

    @Override public void dispose() {}
}
