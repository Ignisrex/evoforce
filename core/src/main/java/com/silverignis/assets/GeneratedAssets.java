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
    private final Texture card;
    private final Texture cardFrame;
    private Texture caveFloorEmissive;

    public GeneratedAssets() {
        this.pixel     = buildPixel();
        this.shadow    = buildShadow();
        this.card      = buildCard();
        this.cardFrame = buildCardFrame();
    }

    public Texture pixel()             { return pixel; }
    public Texture shadow()            { return shadow; }
    public Texture card()              { return card; }
    public Texture cardFrame()         { return cardFrame; }
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

    // Card textures share these dimensions; drawn white so HUDs tint freely.
    private static final int CARD_W = 96, CARD_H = 92, CARD_R = 12;

    /** Filled rounded rect — the dark card body behind a skill icon (drawn tinted). */
    private static Texture buildCard() {
        Pixmap pm = new Pixmap(CARD_W, CARD_H, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();
        pm.setColor(Color.WHITE);
        fillRoundedRect(pm, 0, 0, CARD_W, CARD_H, CARD_R);
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    /**
     * Rounded border-only overlay for the front card: thin light outline, charcoal
     * band, thin light inner line, transparent interior. Drawn over the icon.
     */
    private static Texture buildCardFrame() {
        Pixmap pm = new Pixmap(CARD_W, CARD_H, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        Color light    = new Color(0.62f, 0.65f, 0.72f, 1f);
        Color charcoal = new Color(0.16f, 0.17f, 0.22f, 1f);

        pm.setColor(light);
        fillRoundedRect(pm, 0, 0, CARD_W, CARD_H, CARD_R);
        pm.setColor(charcoal);
        fillRoundedRect(pm, 2, 2, CARD_W - 4, CARD_H - 4, CARD_R - 2);
        pm.setColor(light);
        fillRoundedRect(pm, 7, 7, CARD_W - 14, CARD_H - 14, CARD_R - 6);

        // Punch out the interior so the frame overlays the icon without hiding it.
        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(0f, 0f, 0f, 0f);
        fillRoundedRect(pm, 9, 9, CARD_W - 18, CARD_H - 18, CARD_R - 8);

        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    private static void fillRoundedRect(Pixmap pm, int x, int y, int w, int h, int r) {
        pm.fillRectangle(x + r, y, w - 2 * r, h);
        pm.fillRectangle(x, y + r, w, h - 2 * r);
        pm.fillCircle(x + r, y + r, r);
        pm.fillCircle(x + w - r - 1, y + r, r);
        pm.fillCircle(x + r, y + h - r - 1, r);
        pm.fillCircle(x + w - r - 1, y + h - r - 1, r);
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
        card.dispose();
        cardFrame.dispose();
        if (caveFloorEmissive != null) caveFloorEmissive.dispose();
    }
}
