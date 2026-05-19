package com.silverignis.entities;

/**
 * A {@value #COLS} x {@value #ROWS} grid of panels. Left half belongs to
 * the player, right half to the enemy. Pure geometry + panel-state container;
 * the visible floor is drawn by {@code GameEnvironment}'s 3D panel meshes.
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

    public Battlefield(float x, float y, float panelWidth, float panelHeight, PanelType[][] panels) {
        this.x = x;
        this.y = y;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.panels = panels;
    }

    public float getPanelWidth()         { return panelWidth; }
    public float getPanelHeight()        { return panelHeight; }
    /** Compressed height used for all rendering and entity positioning. */
    public float getPanelRenderHeight()  { return panelHeight * RENDER_HEIGHT_SCALE; }

    /** True if {@code col} is on the player's half of the grid. */
    public boolean isPlayerSide(int col) { return col < COLS / 2; }

    /** World X at the center of the given column. */
    public float tileCenterX(int col) { return x + (col + 0.3f) * panelWidth; }

    /** World Y at the center of the given row (uses the compressed render height). */
    public float tileCenterY(int row) { return y + (row + 0.3f) * getPanelRenderHeight(); }

    public PanelType getPanel(int col, int row) {
        return panels[col][row];
    }

    public void setPanel(int col, int row, PanelType type) {
        panels[col][row] = type;
    }
}
