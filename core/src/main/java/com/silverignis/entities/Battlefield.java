package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * A {@value #COLS} x {@value #ROWS} grid of panels.  Left half belongs to
 * the player, right half to the enemy.
 *
 * <p>Panels are drawn programmatically: a dark fill with a thin coloured
 * border (blue for the player side, red for the enemy side), inspired by
 * the One Step from Eden / Duelists of Eden aesthetic.
 */
public class Battlefield {

    public static final int COLS = 8;
    public static final int ROWS = 4;

    public enum PanelType {
        NORMAL_BLUE,
        NORMAL_RED,
        CRACKED,
        BROKEN,
        ICE,
        LAVA,
        GRASS,
        POISON
    }

    // ── visual constants ──────────────────────────────────────────────
    private static final Color PANEL_FILL      = new Color(0.08f, 0.08f, 0.12f, 0.92f);
    private static final Color BLUE_BORDER     = new Color(0.25f, 0.45f, 0.95f, 1f);
    private static final Color RED_BORDER      = new Color(0.90f, 0.20f, 0.25f, 1f);
    /** Border thickness in pixels inside the generated panel texture. */
    private static final int   TEX_SIZE        = 64;
    private static final int   BORDER_PX       = 2;

    private final PanelType[][] panels;

    /**
     * How much the panel height is squished vertically when drawn.
     * Values below 1.0 make the floor look angled (2.5D perspective illusion).
     * All entity positioning uses {@link #getPanelRenderHeight()} so the visual
     * floor and entity feet always stay aligned.
     */
    private static final float RENDER_HEIGHT_SCALE = 0.60f;

    // World-space placement. (x, y) is the bottom-left corner of the grid.
    private final float x;
    private final float y;
    private final float panelWidth;
    private final float panelHeight;

    /** Pre-baked panel textures: one for the player side, one for the enemy side. */
    private final Texture bluePanel;
    private final Texture redPanel;

    public Battlefield(float x, float y, float panelWidth, float panelHeight, PanelType[][] panels) {
        this.x = x;
        this.y = y;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.panels = panels;

        bluePanel = buildPanelTexture(BLUE_BORDER);
        redPanel  = buildPanelTexture(RED_BORDER);
    }

    public float getX()                  { return x; }
    public float getY()                  { return y; }
    public float getPanelWidth()         { return panelWidth; }
    public float getPanelHeight()        { return panelHeight; }
    /** Compressed height used for all rendering and entity positioning. */
    public float getPanelRenderHeight()  { return panelHeight * RENDER_HEIGHT_SCALE; }

    /** Total grid width in world units (COLS * panelWidth). */
    public float getWorldWidth()  { return COLS * panelWidth; }
    /** Total grid height in world units, using the compressed render height. */
    public float getWorldHeight() { return ROWS * getPanelRenderHeight(); }

    /** True if {@code col} is on the player's half of the grid. */
    public boolean isPlayerSide(int col) { return col < COLS / 2; }

    /** World X at the center of the given column. */
    public float tileCenterX(int col) { return x + (col + 0.5f) * panelWidth; }

    /** World Y at the center of the given row (uses the compressed render height). */
    public float tileCenterY(int row) { return y + (row + 0.5f) * getPanelRenderHeight(); }

    public PanelType getPanel(int col, int row) {
        return panels[col][row];
    }

    public void setPanel(int col, int row, PanelType type) {
        panels[col][row] = type;
    }

    public void render(SpriteBatch batch) {
        Color prev = batch.getColor().cpy();
        // Preserve caller-set alpha so PlayState can render panels semi-transparent.
        batch.setColor(1f, 1f, 1f, prev.a);

        float rh = getPanelRenderHeight();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float px = x + col * panelWidth;
                float py = y + row * rh;
                Texture tex = isPlayerSide(col) ? bluePanel : redPanel;
                batch.draw(tex, px, py, panelWidth, rh);
            }
        }

        batch.setColor(prev);
    }

    public com.badlogic.gdx.math.Rectangle tileHitbox(int col, int row, float inset,
                                                        com.badlogic.gdx.math.Rectangle out) {
        float pw = panelWidth;
        float ph = getPanelRenderHeight();
        float ix = pw * inset;
        float iy = ph * inset;
        return out.set(x + col * pw + ix, y + row * ph + iy, pw - 2f * ix, ph - 2f * iy);
    }

    public void dispose() {
        bluePanel.dispose();
        redPanel.dispose();
    }

    // ── helpers ───────────────────────────────────────────────────────

    /**
     * Creates a {@value #TEX_SIZE}x{@value #TEX_SIZE} texture: dark fill
     * with a thin border of the given colour.
     */
    private static Texture buildPanelTexture(Color border) {
        Pixmap pm = new Pixmap(TEX_SIZE, TEX_SIZE, Pixmap.Format.RGBA8888);

        // Fill with dark panel colour.
        pm.setColor(PANEL_FILL);
        pm.fill();

        // Draw thin border.
        pm.setColor(border);
        for (int b = 0; b < BORDER_PX; b++) {
            pm.drawRectangle(b, b, TEX_SIZE - 2 * b, TEX_SIZE - 2 * b);
        }

        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }
}
