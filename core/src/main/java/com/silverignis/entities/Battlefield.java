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

    public Battlefield(PanelType[][] panels) {
        this.panels = panels;
    }

    // The one construction site passes 10f/COLS and 4f/ROWS, so these are
    // constants in practice. Static, like the grid geometry above, so the render
    // pass can size a panel without being handed the panel-state grid.
    public static final float PANEL_WIDTH  = 10f / COLS;
    public static final float PANEL_HEIGHT = 4f  / ROWS;

    public static float getPanelWidth()  { return PANEL_WIDTH; }
    public static float getPanelHeight() { return PANEL_HEIGHT; }
    /** Compressed height used for all rendering and entity positioning. */
    public static float getPanelRenderHeight() { return PANEL_HEIGHT * RENDER_HEIGHT_SCALE; }

    /** True if {@code col} is on the player's half of the grid. */
    public boolean isPlayerSide(int col) { return col < COLS / 2; }

    // Static: pure functions of the grid constants (Vfx uses them for tile-relative offsets).
    public static float panelFloorWidth() { return GRID_WIDTH_3D / COLS; }
    public static float panelFloorDepth() { return GRID_DEPTH_3D / ROWS; }

    // Grid geometry is a pure function of the constants above, so these are
    // static: tick-time code (hit tests, particle anchors) can locate a tile in
    // the world without holding a Battlefield instance. The instance side of
    // this class is only the panel-state grid and the render sizes.

    /** Floor-space X of a column center (world X for SceneCamera.project).
     *  Takes a float so a projectile mid-flight can sit between two columns. */
    public static float floorX(float col) { return GRID_LEFT_3D + (col + 0.5f) * panelFloorWidth(); }

    /** Floor-space Z of a row center (world Z; nearer rows have larger z).
     *  Takes a float so an entity mid-step can sit between two rows. */
    public static float floorZ(float row) { return GRID_NEAR_3D - (row + 0.5f) * panelFloorDepth(); }

    public PanelType getPanel(int col, int row) {
        return panels[col][row];
    }

    public void setPanel(int col, int row, PanelType type) {
        panels[col][row] = type;
    }
}
