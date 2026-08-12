package com.silverignis.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
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

    private static final float FLOOR_AMP  = 0.35f;
    private static final float FLOOR_FREQ = 0.35f;
    private ValueNoise floorNoise;

    private static final float CEIL_AMP  = 0.4f;
    private static final float CEIL_FREQ = 0.35f;
    private ValueNoise ceilingNoise;

    // Crystal glow state, harvested per rebuild; consumed by atmosphere/motion stages.
    private final Array<ColorAttribute> crystalEmissives = new Array<>();
    private final Array<Color> crystalEmissiveBases = new Array<>();
    private final FloatArray crystalPhases = new FloatArray();
    private final Array<Vector3> crystalPositions = new Array<>();
    private final Vector3 tmpAxis = new Vector3();

    private final CaveTheme theme;
    private final PointLight[] crystalLights = new PointLight[3];
    private static final float LIGHT_BASE_INTENSITY = 10f;
    private float time;
    private long currentSeed = Long.MIN_VALUE;

    private final SceneCamera sceneCamera;
    private final ModelBuilder modelBuilder = new ModelBuilder();

    public GameEnvironment(Viewport viewport, CaveTheme theme) {
        // Theme textures are borrowed from GameAssets / GeneratedAssets (loaded + filtered
        // centrally); this class neither owns nor disposes them.
        this.theme = theme;

        modelBatch = new ModelBatch();

        sceneCamera = new SceneCamera(viewport);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, theme.ambient));
        environment.set(new ColorAttribute(ColorAttribute.Fog, theme.fogColor));
        environment.add(new DirectionalLight().set(0.4f, 0.45f, 0.6f, 0f, -1f, -0.5f));
        for (int i = 0; i < crystalLights.length; i++) {
            crystalLights[i] = new PointLight().set(theme.lightPalette[i], 0f, 1f, 0f, 10f);
            environment.add(crystalLights[i]);
        }
        environment.add(new PointLight().set(new Color(0.4f, 0.5f, 0.8f, 1f), 0f, 3f, 1f, 15f)); // front fill

        rebuild(CaveTheme.OVERWORLD_SEED);
    }

    public void rebuild(long seed) {
        if (seed == currentSeed) return;
        currentSeed = seed;

        for(Model m : models) m.dispose();
        models.clear();
        instances.clear();
        crystalEmissives.clear();
        crystalEmissiveBases.clear();
        crystalPhases.clear();
        crystalPositions.clear();
        buildGeometry( new RandomXS128(seed));

        // Light pools emanate from actual glowing sources; front fill keeps sprites readable.
        for (int i = 0; i < crystalLights.length; i++) {
            Vector3 p = crystalPositions.get(i % crystalPositions.size);
            crystalLights[i].position.set(p.x, p.y + 0.4f, p.z);
        }
    }

    private void buildGeometry(RandomXS128 rng) {
        Material wallMat  = new Material(TextureAttribute.createDiffuse(theme.wallTex));

        Material floorMat = new Material(TextureAttribute.createDiffuse(theme.floorTex));
        if (theme.floorEmissiveTex != null) {
            floorMat.set(TextureAttribute.createEmissive(theme.floorEmissiveTex));
        }

        // floor
        floorNoise = new ValueNoise(rng.nextLong());
        displacedGrid(floorMat, new Vector3(-12f, -0.02f, -8f), new Vector3(0, 0, 1), new Vector3(1, 0, 0),
            20f, 24f, 40, 48, FLOOR_AMP, FLOOR_FREQ, 2.5f, floorNoise,
            (u, v) -> flatMask(-12f + v, -8f + u));

        // Walls face inward: normal = uDir × vDir must point into the room.
        // Edges overlap neighbouring surfaces by more than the ±0.6 relief so gaps can't open.
        displacedGrid(wallMat, new Vector3(-10.8f, -1f, -8f), new Vector3(1, 0, 0), new Vector3(0, 1, 0),
            21.6f, 8f, 40, 16, 0.6f, 0.35f, 4f, new ValueNoise(rng.nextLong()), null);          // back (+z)
        displacedGrid(wallMat, new Vector3(-10f, -1f, 7f), new Vector3(0, 0, -1), new Vector3(0, 1, 0),
            18f, 8f, 36, 16, 0.6f, 0.35f, 4f, new ValueNoise(rng.nextLong()), null);            // left (+x)
        displacedGrid(wallMat, new Vector3(10f, -1f, -11f), new Vector3(0, 0, 1), new Vector3(0, 1, 0),
            18f, 8f, 36, 16, 0.6f, 0.35f, 4f, new ValueNoise(rng.nextLong()), null);            // right (−x)
        ceilingNoise = new ValueNoise(rng.nextLong());
        displacedGrid(wallMat, new Vector3(-10.8f, 5.5f, -4.5f), new Vector3(1, 0, 0), new Vector3(0, 0, 1),
            21.6f, 3f, 40, 8, CEIL_AMP, CEIL_FREQ, 4f, ceilingNoise, null);                     // ceiling (−y)

        buildCrystals(rng);
        buildLoneCrystals(rng);
    }

    // Side wall faces can bulge inward to |x|≈9.4, back wall to z≈-7.4; every spire's
    // base is clamped so base + lean drift + radius stays inside these lines.
    private static final float WALL_CLEAR = 8.8f;
    private static final float BACK_CLEAR = -6.8f;

    // Reference: pink/magenta crystal bodies with bright cyan tips (two-tone per crystal).
    private static final Color PINK_DIFFUSE  = new Color(0.90f, 0.62f, 0.80f, 1f);
    private static final Color PINK_EMISSIVE = new Color(0.60f, 0.28f, 0.48f, 1f);
    private static final Color CYAN_DIFFUSE  = new Color(0.60f, 0.90f, 0.95f, 1f);
    private static final Color CYAN_EMISSIVE = new Color(0.30f, 0.80f, 0.90f, 1f);

    // Perimeter anchor spots for cluster placement — shuffled per rebuild so formations
    // spread around the room instead of clumping on one side.
    // z stays ≥ -4.6: the floor skirt crests around z≈-5 and the camera looks over it,
    // so anything deeper is swallowed by the ridge with only tips poking through.
    private static final float[][] CLUSTER_SPOTS = {
        {-7.4f, -4.4f}, {7.4f, -4.4f}, {-7.9f, -1f}, {7.9f, -1f},
        {-7.2f, 2.6f},  {7.2f, 2.6f},  {-4f, -4.6f}, {4f, -4.6f},
    };

    /** Pointed hexagonal prism, base centred at local origin, tip up. Flat per-face
     *  normals — the stock cylinder/cone builders smooth-shade, which kills the facets.
     *  Sides and tip go to separate parts: pink body, cyan glowing tip (reference look). */
    private void hexSpire(MeshPartBuilder side, MeshPartBuilder tip, float r, float bodyH, float tipH, float angleOffsetDeg) {
        MeshPartBuilder.VertexInfo v1 = new MeshPartBuilder.VertexInfo();
        MeshPartBuilder.VertexInfo v2 = new MeshPartBuilder.VertexInfo();
        MeshPartBuilder.VertexInfo v3 = new MeshPartBuilder.VertexInfo();
        Vector3 apex = new Vector3(0f, bodyH + tipH, 0f);
        Vector3 nrm = new Vector3(), e1 = new Vector3(), e2 = new Vector3();
        for (int k = 0; k < 6; k++) {
            float a0 = (angleOffsetDeg + k * 60f) * MathUtils.degreesToRadians;
            float a1 = (angleOffsetDeg + (k + 1) * 60f) * MathUtils.degreesToRadians;
            Vector3 p0 = new Vector3(MathUtils.cos(a0) * r, 0f, MathUtils.sin(a0) * r);
            Vector3 p1 = new Vector3(MathUtils.cos(a1) * r, 0f, MathUtils.sin(a1) * r);
            Vector3 p0t = new Vector3(p0.x, bodyH, p0.z);
            Vector3 p1t = new Vector3(p1.x, bodyH, p1.z);

            nrm.set(p0).add(p1).scl(0.5f).nor();  // side face normal = outward mid direction
            side.rect(p1, p0, p0t, p1t, nrm);

            e1.set(p0t).sub(p1t);
            e2.set(apex).sub(p1t);
            nrm.set(e1).crs(e2).nor();            // tip facet normal
            v1.setPos(p1t).setNor(nrm);
            v2.setPos(p0t).setNor(nrm);
            v3.setPos(apex).setNor(nrm);
            tip.triangle(v1, v2, v3);
        }
    }

    // Reference look: well-spaced perimeter formations, each one dominant tall spire
    // with small shards hugging its base; pink bodies, bright cyan tips.
    private void buildCrystals(RandomXS128 rng) {
        int[] order = new int[CLUSTER_SPOTS.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        for (int i = order.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = order[i]; order[i] = order[j]; order[j] = t;
        }

        for (int c = 0; c < theme.crystalClusters && c < order.length; c++) {
            float cx = CLUSTER_SPOTS[order[c]][0] + (rng.nextFloat() - 0.5f) * 1.2f;
            float cz = CLUSTER_SPOTS[order[c]][1] + (rng.nextFloat() - 0.5f) * 1.2f;
            for (int t = 0; t < 8 && ridgeHidden(cx, cz); t++) cz += 0.3f;  // walk forward out of dips

            Material pink = new Material(
                ColorAttribute.createDiffuse(PINK_DIFFUSE),
                ColorAttribute.createEmissive(PINK_EMISSIVE));
            Material cyan = new Material(
                ColorAttribute.createDiffuse(CYAN_DIFFUSE),
                ColorAttribute.createEmissive(CYAN_EMISSIVE));
            modelBuilder.begin();
            MeshPartBuilder bp = modelBuilder.part("pink", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, pink);
            MeshPartBuilder bc = modelBuilder.part("cyan", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, cyan);
            Matrix4 m4 = new Matrix4();

            int minions = 2 + rng.nextInt(3);
            for (int i = 0; i <= minions; i++) {
                boolean hero = i == 0;
                float ang = rng.nextFloat() * MathUtils.PI2;
                float rad = hero ? 0f : 0.28f + rng.nextFloat() * 0.30f;
                float sx = cx + MathUtils.cos(ang) * rad;
                float sz = cz + MathUtils.sin(ang) * rad;
                float h = hero ? 1.0f + rng.nextFloat() * 0.5f : 0.30f + rng.nextFloat() * 0.35f;
                float r = hero ? 0.16f + rng.nextFloat() * 0.08f : 0.08f + rng.nextFloat() * 0.06f;
                float tilt = hero ? rng.nextFloat() * 8f : 10f + rng.nextFloat() * 18f;

                float ext = h * MathUtils.sinDeg(tilt) + r;
                sx = MathUtils.clamp(sx, -WALL_CLEAR + ext, WALL_CLEAR - ext);
                sz = Math.max(sz, BACK_CLEAR + ext);
                for (int t = 0; t < 6 && ridgeHidden(sx, sz); t++) sz += 0.25f;
                float ground = floorHeight(sx, sz);

                m4.setToTranslation(sx, ground - 0.04f, sz);
                if (hero) {
                    m4.rotate(Vector3.Y, rng.nextFloat() * 360f).rotate(Vector3.Z, tilt);
                } else {
                    m4.rotate(tmpAxis.set(sz - cz, 0f, -(sx - cx)).nor(), tilt);  // lean away from hero
                }
                bp.setVertexTransform(m4);
                bc.setVertexTransform(m4);
                hexSpire(bp, bc, r, h * 0.70f, h * 0.30f, rng.nextFloat() * 60f);
            }

            Model m = modelBuilder.end();
            models.add(m);
            ModelInstance inst = new ModelInstance(m);
            instances.add(inst);
            float phase = rng.nextFloat() * MathUtils.PI2;
            for (int p = 0; p < 2; p++) {  // pink body + cyan tip parts pulse together, offset
                ColorAttribute em = (ColorAttribute) inst.materials.get(p).get(ColorAttribute.Emissive);
                crystalEmissives.add(em);
                crystalEmissiveBases.add(new Color(em.color));
                crystalPhases.add(phase + p * 1.5f);
            }
            crystalPositions.add(new Vector3(cx, floorHeight(cx, cz) + 0.6f, cz));
        }
    }

    /** Scattered single spires: floor lones near walls/back, hanging ones on the ceiling.
     *  One model, two shared materials (pink bodies / cyan tips) pulsing as two groups. */
    private void buildLoneCrystals(RandomXS128 rng) {
        Material pink = new Material(
            ColorAttribute.createDiffuse(PINK_DIFFUSE),
            ColorAttribute.createEmissive(PINK_EMISSIVE));
        Material cyan = new Material(
            ColorAttribute.createDiffuse(CYAN_DIFFUSE),
            ColorAttribute.createEmissive(CYAN_EMISSIVE));

        modelBuilder.begin();
        MeshPartBuilder bp = modelBuilder.part("pink", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, pink);
        MeshPartBuilder bc = modelBuilder.part("cyan", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, cyan);
        Matrix4 m4 = new Matrix4();

        for (int i = 0; i < theme.loneCrystals; i++) {
            float x = 0f, z = 0f;
            for (int tries = 0; tries < 20; tries++) {
                if (rng.nextBoolean()) {                               // side strips
                    x = (rng.nextBoolean() ? 1f : -1f) * (6.5f + rng.nextFloat() * 1.8f);
                    z = -4.6f + rng.nextFloat() * 8.6f;
                } else {                                               // back strip, in front of the skirt crest (z≈-5)
                    x = rng.nextFloat() * 16f - 8f;
                    z = -4.8f + rng.nextFloat() * 0.9f;
                }
                if (!ridgeHidden(x, z)) break;
            }
            float h = 0.3f + rng.nextFloat() * 0.6f;
            float r = 0.07f + rng.nextFloat() * 0.09f;
            float tiltZ = (rng.nextFloat() - 0.5f) * 24f;
            float ext = h * MathUtils.sinDeg(Math.abs(tiltZ)) + r;
            x = MathUtils.clamp(x, -WALL_CLEAR + ext, WALL_CLEAR - ext);
            z = Math.max(z, BACK_CLEAR + ext);
            m4.setToTranslation(x, floorHeight(x, z) - 0.03f, z)
              .rotate(Vector3.Y, rng.nextFloat() * 360f)
              .rotate(Vector3.Z, tiltZ);
            bp.setVertexTransform(m4);
            bc.setVertexTransform(m4);
            hexSpire(bp, bc, r, h * 0.65f, h * 0.35f, rng.nextFloat() * 60f);
        }

        for (int i = 0; i < theme.hangingCrystals; i++) {
            float x = rng.nextFloat() * 17f - 8.5f;
            float z = -4.4f + rng.nextFloat() * 2.8f;                  // within ceiling span
            float h = 0.4f + rng.nextFloat() * 0.7f;
            float r = 0.07f + rng.nextFloat() * 0.10f;
            float tiltZ = (rng.nextFloat() - 0.5f) * 20f;
            float ext = h * MathUtils.sinDeg(Math.abs(tiltZ)) + r;
            x = MathUtils.clamp(x, -WALL_CLEAR + ext, WALL_CLEAR - ext);
            m4.setToTranslation(x, ceilingHeight(x, z) + 0.05f, z)     // seated on the actual ceiling surface
              .rotate(Vector3.X, 180f)
              .rotate(Vector3.Y, rng.nextFloat() * 360f)
              .rotate(Vector3.Z, tiltZ);
            bp.setVertexTransform(m4);
            bc.setVertexTransform(m4);
            hexSpire(bp, bc, r, h * 0.55f, h * 0.45f, rng.nextFloat() * 60f);
        }

        Model m = modelBuilder.end();
        models.add(m);
        ModelInstance inst = new ModelInstance(m);
        instances.add(inst);
        float phase = rng.nextFloat() * MathUtils.PI2;
        for (int p = 0; p < 2; p++) {
            ColorAttribute em = (ColorAttribute) inst.materials.get(p).get(ColorAttribute.Emissive);
            crystalEmissives.add(em);
            crystalEmissiveBases.add(new Color(em.color));
            crystalPhases.add(phase + p * 2.2f);
        }
    }



    /** 2D seeded value noise; fbm() layers 3 octaves, output ~[0,1]. */
    private static final class ValueNoise {
        private final long seed;
        ValueNoise(long seed) { this.seed = seed; }

        private float lattice(int x, int y) {
            long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 31;
            return (h & 0xFFFFFF) / (float) 0x1000000;
        }

        float at(float x, float y) {
            int xi = MathUtils.floor(x), yi = MathUtils.floor(y);
            float fx = x - xi, fy = y - yi;
            fx = fx * fx * (3f - 2f * fx);
            fy = fy * fy * (3f - 2f * fy);
            float a = lattice(xi, yi),     b = lattice(xi + 1, yi);
            float c = lattice(xi, yi + 1), d = lattice(xi + 1, yi + 1);
            return MathUtils.lerp(MathUtils.lerp(a, b, fx), MathUtils.lerp(c, d, fx), fy);
        }

        float fbm(float x, float y) {
            return (at(x, y) + 0.5f * at(x * 2.13f, y * 2.13f) + 0.25f * at(x * 4.31f, y * 4.31f)) / 1.75f;
        }
    }

    private float flatMask(float x, float z) {
        float dx = Math.max(0f, Math.abs(x) - 6.2f);
        float dz = Math.max(0f, Math.max(-3.5f - z, z - 3.2f));
        float t = MathUtils.clamp((float) Math.sqrt(dx * dx + dz * dz) / 1.5f, 0f, 1f);
        return t * t * (3f -2f * t);
    }

    private float floorHeight(float x, float z) {
        return -0.02f + FLOOR_AMP * (floorNoise.fbm((z + 8f) * FLOOR_FREQ, (x + 12f) * FLOOR_FREQ) * 2f - 1f) * flatMask(x, z);
    }

    /** True if terrain blocks the camera's line of sight to this spot's base —
     *  a crystal here would show only its tip poking over the ridge. Marches the
     *  actual camera ray; height-vs-neighbour checks miss grazing occlusion. */
    private boolean ridgeHidden(float x, float z) {
        float g = floorHeight(x, z) + 0.05f;
        float dx = x - SceneCamera.CAM_X, dy = g - SceneCamera.CAM_Y, dz = z - SceneCamera.CAM_Z;
        for (float t = 0.5f; t < 0.98f; t += 0.03f) {
            float sy = SceneCamera.CAM_Y + dy * t;
            if (sy > 0.6f) continue;  // ray still well above any terrain (max relief ~0.35)
            // 0.12 margin: the rendered mesh interpolates between 0.5-unit vertices and can
            // sit slightly above the analytic noise height between them.
            if (floorHeight(SceneCamera.CAM_X + dx * t, SceneCamera.CAM_Z + dz * t) > sy - 0.12f) return true;
        }
        return false;
    }

    /** Ceiling surface height at (x, z) — matches the displaced ceiling grid exactly. */
    private float ceilingHeight(float x, float z) {
        return 5.5f - CEIL_AMP * (ceilingNoise.fbm((x + 10.8f) * CEIL_FREQ, (z + 4.5f) * CEIL_FREQ) * 2f - 1f);
    }

    private interface DispMask { float at(float u, float v); }

    /** Subdivided plane at origin + uDir*u + vDir*v, displaced ±amp along uDir×vDir
     *  by noise (scaled by mask if given). UVs tile every uvWorldSize world units. */
    private void displacedGrid(Material mat, Vector3 origin, Vector3 uDir, Vector3 vDir,
                               float uLen, float vLen, int uSegs, int vSegs,
                               float amp, float noiseFreq, float uvWorldSize,
                               ValueNoise noise, DispMask mask) {
        Vector3 n = new Vector3(uDir).crs(vDir).nor();
        float du = uLen / uSegs, dv = vLen / vSegs;

        float[][] disp = new float[uSegs + 1][vSegs + 1];
        for (int iu = 0; iu <= uSegs; iu++)
            for (int iv = 0; iv <= vSegs; iv++) {
                float d = amp * (noise.fbm(iu * du * noiseFreq, iv * dv * noiseFreq) * 2f - 1f);
                disp[iu][iv] = mask == null ? d : d * mask.at(iu * du, iv * dv);
            }

        modelBuilder.begin();
        MeshPartBuilder b = modelBuilder.part("grid", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat);

        short[][] idx = new short[uSegs + 1][vSegs + 1];
        Vector3 pos = new Vector3(), nor = new Vector3();
        Vector2 uv = new Vector2();
        for (int iu = 0; iu <= uSegs; iu++)
            for (int iv = 0; iv <= vSegs; iv++) {
                pos.set(origin)
                   .mulAdd(uDir, iu * du)
                   .mulAdd(vDir, iv * dv)
                   .mulAdd(n, disp[iu][iv]);
                float slopeU = (disp[Math.min(iu + 1, uSegs)][iv] - disp[Math.max(iu - 1, 0)][iv])
                             / (du * (iu == 0 || iu == uSegs ? 1f : 2f));
                float slopeV = (disp[iu][Math.min(iv + 1, vSegs)] - disp[iu][Math.max(iv - 1, 0)])
                             / (dv * (iv == 0 || iv == vSegs ? 1f : 2f));
                nor.set(n).mulAdd(uDir, -slopeU).mulAdd(vDir, -slopeV).nor();
                uv.set(iu * du / uvWorldSize, iv * dv / uvWorldSize);
                idx[iu][iv] = b.vertex(pos, nor, null, uv);
            }

        for (int iu = 0; iu < uSegs; iu++)
            for (int iv = 0; iv < vSegs; iv++) {
                b.triangle(idx[iu][iv], idx[iu + 1][iv], idx[iu + 1][iv + 1]);
                b.triangle(idx[iu][iv], idx[iu + 1][iv + 1], idx[iu][iv + 1]);
            }

        Model m = modelBuilder.end();
        models.add(m);
        instances.add(new ModelInstance(m));
    }

    // Breathing glow: ~10s period, swing 0.35..1.2 of base (peak overdrives past 1 so
    // the bloom halo visibly swells); light pools ride the same wave plus a soft flicker.
    private static final float PULSE_SPEED = 0.6f;

    /** Living-cave motion: crystals and their light pools breathe on one slow wave. */
    public void update(float delta) {
        time += delta;
        for (int i = 0; i < crystalLights.length; i++) {
            float phase = i * 2 < crystalPhases.size ? crystalPhases.get(i * 2) : i * 2.1f;
            float wave = 0.5f + 0.5f * MathUtils.sin(time * PULSE_SPEED + phase);
            float flicker = 1f + 0.03f * MathUtils.sin(time * 7.3f + i * 2.1f)
                               + 0.02f * MathUtils.sin(time * 13.7f + i);
            crystalLights[i].intensity = LIGHT_BASE_INTENSITY * (0.45f + 0.9f * wave) * flicker;
        }
        for (int i = 0; i < crystalEmissives.size; i++) {
            float wave = 0.5f + 0.5f * MathUtils.sin(time * PULSE_SPEED + crystalPhases.get(i));
            float pulse = 0.35f + 0.85f * wave;
            crystalEmissives.get(i).color.set(crystalEmissiveBases.get(i)).mul(pulse, pulse, pulse, 1f);
        }
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
