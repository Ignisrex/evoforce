package com.silverignis.assets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

/**
 * Owns the runtime-generated (Pixmap-built) textures that {@link GameAssets}'s
 * {@code AssetManager} cannot manage — it is file-based and has no loader for
 * procedural content. Two textures:
 * <ul>
 *   <li>{@code pixel} — a single shared 1x1 white texture, drawn tinted for solid
 *       fills/bars by the HUDs (replacing the per-HUD copies).</li>
 *   <li>{@code shadow} — the soft radial ellipse drawn under combatants.</li>
 * </ul>
 * One instance, shared across screens, disposed once. Keep these out of the
 * AssetManager: registering a self-built {@link Texture} there invites double-dispose.
 */
public final class GeneratedAssets implements Disposable {

    private final Texture pixel;
    private final Texture shadow;

    public GeneratedAssets() {
        this.pixel  = buildPixel();
        this.shadow = buildShadow();
    }

    public Texture pixel()  { return pixel; }
    public Texture shadow() { return shadow; }

    private static Texture buildPixel() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture tex = new Texture(p);
        p.dispose();
        return tex;
    }

    /** Soft black ellipse: alpha falls off radially from the centre. (Moved from PlayState.) */
    private static Texture buildShadow() {
        int w = 64, h = 32;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float nx = (px - w * 0.5f) / (w * 0.5f);
                float ny = (py - h * 0.5f) / (h * 0.5f);
                float d  = nx * nx + ny * ny;
                if (d < 1f) {
                    pm.setColor(0f, 0f, 0f, (1f - d) * 0.6f);
                    pm.drawPixel(px, py);
                }
            }
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    @Override
    public void dispose() {
        pixel.dispose();
        shadow.dispose();
    }
}
