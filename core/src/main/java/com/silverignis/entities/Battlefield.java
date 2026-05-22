package com.silverignis.entities;

/**
 * A {@value #COLS} x {@value #ROWS} grid of panels. Left half belongs to
 * the player, right half to the enemy. Pure geometry + panel-state container;
 * the visible floor is drawn by {@code GameEnvironment}'s 3D panel meshes.
 */
public class Battlefield {

    public static final int COLS = 8;
    public static final int ROWS = 4;


    // ── 3D grid layout on the floor (y = 0) ──────────────────────────────
    // Where the battlefield grid sits in 3D space. Intentionally wider/deeper
    // than the 2D logical bounds for perspective fit — tune visually.
    private static final float GRID_LEFT_3D  = -5.5f;  // x of col-0 left edge
    private static final float GRID_WIDTH_3D = 11f;    // total width across all cols
    private static final float GRID_NEAR_3D  =  2.0f;  // z of row-0 front edge (closest to cam)
    private static final float GRID_DEPTH_3D =  5.0f;  // total depth across all rows


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

    public float panelFloorWidth() { return GRID_WIDTH_3D / COLS; }
    public float panelFloorDepth() { return GRID_DEPTH_3D / ROWS; }

    /** Floor-space X of a column center (world X for SceneCamera.project). */
    public float floorX(int col) { return GRID_LEFT_3D + (col + 0.5f) * panelFloorWidth(); }

    /** Floor-space Z of a row center (world Z; nearer rows have larger z). */
    public float floorZ(int row) { return GRID_NEAR_3D - (row + 0.5f) * panelFloorDepth(); }

    public PanelType getPanel(int col, int row) {
        return panels[col][row];
    }

    public void setPanel(int col, int row, PanelType type) {
        panels[col][row] = type;
    }
}
