package com.silverignis.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.entities.Battlefield;

public class GameEnvironment implements Disposable {

    // ── Camera ────────────────────────────────────────────────────────────
    private static final float CAM_X   =  0f;
    private static final float CAM_Y   =  6.75f;
    private static final float CAM_Z   =  7.54f;
    private static final float CAM_FOV = 45f;   // wider to show full grid without side-clipping

    // lookAt = camera_pos + direction*10 for exactly ~40° below horizontal
    // direction = (0, -sin40°, -cos40°) → lookAt ≈ (0, 0.33, -0.11)
    private static final float LOOK_X  =  0f;
    private static final float LOOK_Y  =  0.33f;
    private static final float LOOK_Z  = -0.11f;

    // ── 3D grid layout on the floor (y = 0) ──────────────────────────────
    // Where the battlefield grid sits in 3D space. Intentionally wider/deeper
    // than the 2D logical bounds for perspective fit — tune visually.
    private static final float GRID_LEFT_3D  = -5.5f;  // x of col-0 left edge
    private static final float GRID_WIDTH_3D = 11f;    // total width across all cols
    private static final float GRID_NEAR_3D  =  2.0f;  // z of row-0 front edge (closest to cam)
    private static final float GRID_DEPTH_3D =  5.0f;  // total depth across all rows

    // Back row sprites appear this fraction smaller than front row.
    private static final float DEPTH_SCALE_FAR = 0.22f;

    private final Battlefield battlefield;
    private final Viewport viewport;

    private final float panelW3D;
    private final float panelD3D;

    private final ModelBatch modelBatch;
    private final PerspectiveCamera cam3D;
    private final Environment environment;

    private final Array<Model>         models    = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();

    private final Texture wallTex;
    private final Texture floorTex;

    // Reused temp vector to avoid per-frame allocation in projection methods
    private final Vector3 tmpV3 = new Vector3();

    public GameEnvironment(Battlefield battlefield, Viewport viewport) {
        this.battlefield = battlefield;
        this.viewport    = viewport;
        this.panelW3D    = GRID_WIDTH_3D / Battlefield.COLS;
        this.panelD3D    = GRID_DEPTH_3D / Battlefield.ROWS;
        wallTex  = new Texture(Gdx.files.internal("cave_wall.png"));
        floorTex = new Texture(Gdx.files.internal("cave_floor.png"));
        wallTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        floorTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        modelBatch = new ModelBatch();

        cam3D = new PerspectiveCamera(CAM_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam3D.position.set(CAM_X, CAM_Y, CAM_Z);
        cam3D.lookAt(LOOK_X, LOOK_Y, LOOK_Z);
        cam3D.near = 0.1f;
        cam3D.far  = 100f;
        cam3D.update();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.25f, 0.25f, 0.35f, 1f));
        environment.add(new DirectionalLight().set(0.4f, 0.45f, 0.6f, 0f, -1f, -0.5f));
        environment.add(new PointLight().set(new Color(0.2f, 0.5f, 1.0f, 1f), -3f,  1f,  -4f, 12f));
        environment.add(new PointLight().set(new Color(0.3f, 0.7f, 0.5f, 1f),  3f,  0.5f,-5f, 10f));
        environment.add(new PointLight().set(new Color(0.5f, 0.3f, 0.8f, 1f),  0f,  2f,  -3f,  8f));
        environment.add(new PointLight().set(new Color(0.4f, 0.5f, 0.8f, 1f),  0f,  3f,   1f, 15f)); // front fill

        buildGeometry();
    }

    private void buildGeometry() {
        ModelBuilder mb = new ModelBuilder();
        long attrs = Usage.Position | Usage.Normal | Usage.TextureCoordinates;
        long colorAttrs = Usage.Position | Usage.Normal;

        Material wallMat  = new Material(TextureAttribute.createDiffuse(wallTex));
        Material floorMat = new Material(TextureAttribute.createDiffuse(floorTex));
        Material darkMat  = new Material(ColorAttribute.createDiffuse(0.05f, 0.05f, 0.08f, 1f));

        // Cave structure
        addBox(mb, floorMat,  attrs, 14f, 0.15f, 14f,  0f,  -0.1f,  2f);   // floor
        addBox(mb, wallMat,   attrs, 14f, 8f,    0.1f,  0f,    3f,   -6f);   // back wall
        addBox(mb, wallMat,   attrs, 14f, 0.5f,  3f,    0f,    5.5f, -3f);   // ceiling
        addBox(mb, wallMat,   attrs,  0.2f, 8f, 14f,   -7f,    3f,   -1f);   // left wall  z∈[-8,6]
        addBox(mb, wallMat,   attrs,  0.2f, 8f, 14f,    7f,    3f,   -1f);   // right wall z∈[-8,6]

        // Stalactites
        float[][] stalPos = {
            {-5f, 4.5f, -3.5f}, { 5f, 4.5f, -4f},
            {-2f, 5f,   -5f},   { 2f, 4.8f, -3f},
            {-4f, 4.6f, -5.5f}, { 4f, 5f,   -5f},
            { 0f, 4.7f, -4.5f}, {-1f, 4.5f, -2.5f}
        };
        float[] stalH = {1.0f, 0.8f, 1.3f, 0.7f, 1.1f, 0.9f, 1.4f, 0.6f};
        float[] stalR = {0.15f, 0.12f, 0.18f, 0.10f, 0.16f, 0.13f, 0.20f, 0.09f};
        for (int i = 0; i < stalPos.length; i++) {
            Model cone = mb.createCone(stalR[i] * 2, stalH[i], stalR[i] * 2, 8, darkMat, colorAttrs);
            models.add(cone);
            ModelInstance inst = new ModelInstance(cone);
            inst.transform.setToTranslation(stalPos[i][0], stalPos[i][1] - stalH[i] * 0.5f, stalPos[i][2]);
            instances.add(inst);
        }

        // ── Battlefield grid panels — flat slabs on the floor ─────────────
        // Dark fill with blue (player side) or red (enemy side) specular so
        // the crystal point lights cast the correct glow colour on each tile.
        Material bluePanelMat = new Material(
            ColorAttribute.createDiffuse(0.18f, 0.22f, 0.50f, 1f),
            ColorAttribute.createSpecular(0.30f, 0.55f, 1.00f, 1f));
        Material redPanelMat = new Material(
            ColorAttribute.createDiffuse(0.50f, 0.16f, 0.18f, 1f),
            ColorAttribute.createSpecular(1.00f, 0.25f, 0.30f, 1f));

        float gap = 0.04f;
        float panelH = 0.03f;
        float panelY = panelH * 0.5f;
        for (int col = 0; col < Battlefield.COLS; col++) {
            for (int row = 0; row < Battlefield.ROWS; row++) {
                float cx = GRID_LEFT_3D + (col + 0.5f) * panelW3D;
                float cz = GRID_NEAR_3D - (row + 0.5f) * panelD3D;
                Material mat = battlefield.isPlayerSide(col) ? bluePanelMat : redPanelMat;
                addBox(mb, mat, colorAttrs,
                    panelW3D - gap, panelH, panelD3D - gap,
                    cx, panelY, cz);
            }
        }
    }

    private void addBox(ModelBuilder mb, Material mat, long attrs,
                        float w, float h, float d, float x, float y, float z) {
        Model m = mb.createBox(w, h, d, mat, attrs);
        models.add(m);
        ModelInstance inst = new ModelInstance(m);
        inst.transform.setToTranslation(x, y, z);
        instances.add(inst);
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    public void render(int screenW, int screenH) {
        Gdx.gl.glViewport(0, 0, screenW, screenH);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        cam3D.viewportWidth  = screenW;
        cam3D.viewportHeight = screenH;
        cam3D.update();

        modelBatch.begin(cam3D);
        for (ModelInstance inst : instances) {
            modelBatch.render(inst, environment);
        }
        modelBatch.end();
    }

    public void resize(int w, int h) {
        cam3D.viewportWidth  = w;
        cam3D.viewportHeight = h;
        cam3D.update();
    }

    // ── Projection utilities ──────────────────────────────────────────────

    /**
     * Projects a tile center onto the 3D floor and returns the 2D viewport
     * world position where a sprite should be drawn to appear standing on it.
     */
    public Vector2 projectTile(int col, int row) {
        float x3D = GRID_LEFT_3D + (col + 0.5f) * panelW3D;
        float z3D = GRID_NEAR_3D - (row + 0.5f) * panelD3D;

        tmpV3.set(x3D, 0f, z3D);
        cam3D.project(tmpV3);

        // cam3D.project() and viewport world both use y=0 at bottom — no flip.
        float worldX = (tmpV3.x - viewport.getScreenX()) / viewport.getScreenWidth()  * viewport.getWorldWidth();
        float worldY = (tmpV3.y - viewport.getScreenY()) / viewport.getScreenHeight() * viewport.getWorldHeight();
        return new Vector2(worldX, worldY);
    }

    /**
     * Perspective depth scale for a row: 1.0 at the nearest row, decreasing
     * linearly toward {@link #DEPTH_SCALE_FAR} at the farthest. Multiply
     * sprite size by this to make far entities appear smaller.
     */
    public float tileDepthScale(int row) {
        float ny = (row + 0.5f) / Battlefield.ROWS;  // 0=near, 1=far
        return 1.0f - ny * DEPTH_SCALE_FAR;
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        wallTex.dispose();
        floorTex.dispose();
        for (Model m : models) m.dispose();
    }
}
