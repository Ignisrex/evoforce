package com.silverignis.environment;

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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameEnvironment implements Disposable {

    // Back row sprites appear this fraction smaller than front row.
    private static final float DEPTH_SCALE_FAR = 0.22f;

    private final ModelBatch modelBatch;
    private final Environment environment;

    private final Array<Model>         models    = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();
    private final Array<Model> decorModels = new Array<>();
    private final Array<ModelInstance> decorInstances = new Array<>();

    // Floor texture is 4x tiled across the 22x18 floor box (~5 world units per repeat).
    private static final float FLOOR_UV_SCALE = 4f;

    private final Texture wallTex;
    private final Texture floorTex;
    private final Texture floorEmissiveTex;

    private final SceneCamera sceneCamera;
    private final ModelBuilder modelBuilder = new ModelBuilder();

    public GameEnvironment(Viewport viewport, Texture wallTex, Texture floorTex, Texture floorEmissiveTex) {
        // Cave textures are borrowed from GameAssets / GeneratedAssets (loaded + filtered
        // centrally); this class neither owns nor disposes them. floorEmissiveTex may be null.
        this.wallTex          = wallTex;
        this.floorTex         = floorTex;
        this.floorEmissiveTex = floorEmissiveTex;

        modelBatch = new ModelBatch();

        sceneCamera = new SceneCamera(viewport);

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
        long attrs = Usage.Position | Usage.Normal | Usage.TextureCoordinates;
        long colorAttrs = Usage.Position | Usage.Normal;

        Material wallMat  = new Material(TextureAttribute.createDiffuse(wallTex));

        TextureAttribute floorDiffuse = TextureAttribute.createDiffuse(floorTex);
        floorDiffuse.scaleU = FLOOR_UV_SCALE;
        floorDiffuse.scaleV = FLOOR_UV_SCALE;
        Material floorMat = new Material(floorDiffuse);
        if (floorEmissiveTex != null) {
            TextureAttribute floorEmissive = TextureAttribute.createEmissive(floorEmissiveTex);
            floorEmissive.scaleU = FLOOR_UV_SCALE;
            floorEmissive.scaleV = FLOOR_UV_SCALE;
            floorMat.set(floorEmissive);
        }

        Material darkMat  = new Material(ColorAttribute.createDiffuse(0.05f, 0.05f, 0.08f, 1f));

        // Cave structure — enlarged. Floor is wider+deeper than the wall enclosure so its
        // edges stay hidden behind/under the walls. Battlefield grid (x ±5.5, z -3..+2) sits
        // well within the new bounds.
        addBox(modelBuilder, floorMat,  attrs, 220f, 0.15f, 18f,   0f,  -0.1f,  2f);   // floor
        addBox(modelBuilder, wallMat,   attrs, 20f, 8f,    0.1f,  0f,    3f,   -8f);   // back wall
        addBox(modelBuilder, wallMat,   attrs, 20f, 0.5f,  3f,    0f,    5.5f, -3f);   // ceiling
        addBox(modelBuilder, wallMat,   attrs,  0.2f, 8f, 18f,  -10f,    3f,   -2f);   // left wall  z∈[-11,7]
        addBox(modelBuilder, wallMat,   attrs,  0.2f, 8f, 18f,   10f,    3f,   -2f);   // right wall z∈[-11,7]

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
            Model cone = modelBuilder.createCone(stalR[i] * 2, stalH[i], stalR[i] * 2, 8, darkMat, colorAttrs);
            models.add(cone);
            ModelInstance inst = new ModelInstance(cone);
            inst.transform.setToTranslation(stalPos[i][0], stalPos[i][1] - stalH[i] * 0.5f, stalPos[i][2]);
            instances.add(inst);
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

        PerspectiveCamera cam = sceneCamera.getCamera();
        cam.viewportWidth  = screenW;
        cam.viewportHeight = screenH;
        cam.update();

        modelBatch.begin(cam);
        for (ModelInstance inst : instances) {
            modelBatch.render(inst, environment);
        }
        for (ModelInstance inst : decorInstances) modelBatch.render(inst, environment);
        modelBatch.end();
    }

    public void resize(int w, int h) {
        sceneCamera.resize(w, h);
    }

    public Vector2 project(float worldX, float worldZ){ return sceneCamera.project(worldX, worldZ); }

    public Vector2 project(float worldX, float worldZ, Vector2 out) {
        return sceneCamera.project(worldX, worldZ, out);
    }

    public float depthScale(float worldZ) { return sceneCamera.depthScale(worldZ); }

    public float unprojectX(float viewportX, float worldZ) { return sceneCamera.unprojectX(viewportX, worldZ); }

    public float unprojectHeight(float viewportY, float worldX, float worldZ) {
        return sceneCamera.unprojectHeight(viewportY, worldX, worldZ);
    }

    public ModelInstance addDecor(Material mat, float w, float h, float d, float x, float y, float z) {
        Model m = modelBuilder.createBox(w, h, d, mat, Usage.Position | Usage.Normal);
        decorModels.add(m);
        ModelInstance inst = new ModelInstance(m);
        inst.transform.setToTranslation(x, y, z);
        decorInstances.add(inst);
        return inst;
    }

    public void clearDecor() {
        for (Model m : decorModels) m.dispose();
        decorModels.clear();
        decorInstances.clear();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        for (Model m : models) m.dispose();
        for (Model m : decorModels) m.dispose();
    }
}
