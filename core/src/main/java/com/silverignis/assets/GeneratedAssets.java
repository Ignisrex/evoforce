package com.silverignis.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

/**
 * Owns the runtime-generated (Pixmap-built) textures that {@link GameAssets}'s
 * {@code AssetManager} cannot manage — it is file-based and has no loader for
 * procedural content. Textures:
 * <ul>
 *   <li>{@code pixel} — a single shared 1x1 white texture, drawn tinted for solid
 *       fills/bars by the HUDs (replacing the per-HUD copies).</li>
 *   <li>{@code shadow} — the soft radial ellipse drawn under combatants.</li>
 *   <li>{@code caveFloorEmissive} — derived from {@code cave_floor.png}; bright cyan
 *       crystal-vein pixels copied onto black for the floor material's emissive slot.</li>
 * </ul>
 * One instance, shared across screens, disposed once. Keep these out of the
 * AssetManager: registering a self-built {@link Texture} there invites double-dispose.
 */
public final class GeneratedAssets implements Disposable {

    // (b+g)/2 must exceed this for a diffuse pixel to count as "glow".
    private static final float EMISSIVE_BRIGHTNESS_MIN = 0.40f;
    // (b+g)/2 must exceed r by this margin — filters out neutral-bright stone.
    private static final float EMISSIVE_BG_DOMINANCE   = 0.15f;

    private final Texture pixel;
    private final Texture shadow;
    private Texture caveFloorEmissive;

    public GeneratedAssets() {
        this.pixel  = buildPixel();
        this.shadow = buildShadow();
    }

    public Texture pixel()             { return pixel; }
    public Texture shadow()            { return shadow; }
    public Texture caveFloorEmissive() { return caveFloorEmissive; }

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

    /**
     * Derive the floor emissive map by walking {@code diffusePath} and copying any pixel
     * whose blue+green channels dominate red onto a black background. Resulting texture
     * is mipmap-filtered + Repeat-wrapped so it tiles in lockstep with the diffuse.
     * No-op if the file is missing.
     */
    public void buildCaveFloorEmissive(String diffusePath) {
        FileHandle fh = Gdx.files.internal(diffusePath);
        if (!fh.exists()) return;
        Pixmap src = new Pixmap(fh);
        Pixmap dst = new Pixmap(src.getWidth(), src.getHeight(), Pixmap.Format.RGBA8888);
        dst.setColor(0f, 0f, 0f, 1f);
        dst.fill();
        Color tmp = new Color();
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                Color.rgba8888ToColor(tmp, src.getPixel(x, y));
                float bg = (tmp.b + tmp.g) * 0.5f;
                if (bg > EMISSIVE_BRIGHTNESS_MIN && (bg - tmp.r) > EMISSIVE_BG_DOMINANCE) {
                    dst.setColor(tmp.r, tmp.g, tmp.b, 1f);
                    dst.drawPixel(x, y);
                }
            }
        }
        caveFloorEmissive = new Texture(dst, true);
        caveFloorEmissive.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        caveFloorEmissive.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        src.dispose();
        dst.dispose();
    }

    @Override
    public void dispose() {
        pixel.dispose();
        shadow.dispose();
        if (caveFloorEmissive != null) caveFloorEmissive.dispose();
    }
}
