package com.silverignis.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.entities.Battlefield;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * LIGHTNING: the tile stays a normal team slab under an electrified field —
 * several sustained currents spanning the face in varied directions, their
 * fixed control paths writhing with time-varying displacement (morph, don't
 * re-roll), plus sparse transient discharges. Thin-but-visible camera-facing
 * ribbons in near-white so the bloom pass does the glowing.
 */
public class LightningPanelSurface extends PanelSurface {

    private static final float TRANSIENT_REFRESH = 0.28f;
    private static final int MAX_VERTS = 1200;
    private static final float BASE_Y = 0.045f;

    private static class Bolt {
        float[] pts;        // rest x,y,z triples
        float width;
        float phase;
        float amp;
        boolean sustained;
    }

    private final ShaderProgram shader;
    private final Mesh[][] meshes = new Mesh[Battlefield.COLS][Battlefield.ROWS];
    @SuppressWarnings("unchecked")
    private final List<Bolt>[][] bolts = new List[Battlefield.COLS][Battlefield.ROWS];
    private final int[][] counts = new int[Battlefield.COLS][Battlefield.ROWS];
    private final float[] scratch = new float[MAX_VERTS * 5];   // pos3 + color + edge
    private final Random rng = new Random();
    private float lastTransient = -1f;

    public LightningPanelSurface() {
        super(Battlefield.PanelType.LIGHTNING);
        this.shader = ShaderPanelSurface.load("arc");
    }

    @Override public void tileGained(int col, int row) {
        meshes[col][row] = new Mesh(false, MAX_VERTS, 0,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_edge"));
        List<Bolt> list = new ArrayList<>();
        float hx = halfX(), hz = halfZ();
        int spans = 1 + rng.nextInt(2);                               // only a few cross the whole tile
        for (int k = 0; k < spans; k++) list.add(fieldBolt(hx, hz, rng.nextInt(4)));
        int arches = 4 + rng.nextInt(3);                              // the main texture: local up-and-over arcs
        for (int k = 0; k < arches; k++) list.add(archBolt(hx, hz));
        bolts[col][row] = list;
        rollTransients(list, hx, hz);
    }

    @Override public void tileLost(int col, int row) {
        if (meshes[col][row] != null) { meshes[col][row].dispose(); meshes[col][row] = null; }
        bolts[col][row] = null;
    }

    @Override public void render(Camera cam) {
        Battlefield bf = host.battlefield();
        float time = host.time();
        boolean reroll = time - lastTransient >= TRANSIENT_REFRESH;
        if (reroll) lastTransient = time;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);   // additive: overlaps brighten, edges melt
        Gdx.gl.glDepthMask(false);
        boolean bound = false;
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                if (bf.getPanel(c, r) != type || meshes[c][r] == null) continue;
                if (reroll) rollTransients(bolts[c][r], halfX(), halfZ());
                rebuild(c, r, cam, time);
                if (!bound) {
                    shader.bind();
                    host.commonUniforms(shader, cam);
                    bound = true;
                }
                shader.setUniformf("u_tileCenter", Battlefield.floorX(c), Battlefield.floorZ(r));
                meshes[c][r].render(shader, GL20.GL_TRIANGLES, 0, counts[c][r]);
            }
        }
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private static float halfX() { return Battlefield.panelFloorWidth() * 0.5f - 0.035f; }
    private static float halfZ() { return Battlefield.panelFloorDepth() * 0.5f - 0.035f; }

    /** One sustained current spanning the face. Endpoints are fully independent —
     *  no mirroring, so spans don't all funnel through the tile center. */
    private Bolt fieldBolt(float hx, float hz, int pattern) {
        int n = 14;
        float x0, z0, x1, z1;
        switch (pattern) {
            case 0:  x0 = -hx; x1 = hx; z0 = rnd(hz * 0.85f); z1 = rnd(hz * 0.85f); break;   // across
            case 1:  z0 = -hz; z1 = hz; x0 = rnd(hx * 0.85f); x1 = rnd(hx * 0.85f); break;   // front-back
            case 2:  x0 = -hx; z0 = -hz + rng.nextFloat() * 0.5f;
                     x1 =  hx; z1 =  hz - rng.nextFloat() * 0.5f; break;                     // diagonal \
            default: x0 = -hx; z0 =  hz - rng.nextFloat() * 0.5f;
                     x1 =  hx; z1 = -hz + rng.nextFloat() * 0.5f; break;                     // diagonal /
        }
        Bolt b = new Bolt();
        b.pts = new float[n * 3];
        int i = 0;
        for (int k = 0; k < n; k++) {
            float t = k / (float) (n - 1);
            boolean mid = k > 0 && k < n - 1;
            float x = MathUtils.lerp(x0, x1, t) + (mid ? (rng.nextFloat() - 0.5f) * 0.12f : 0f);
            float z = MathUtils.lerp(z0, z1, t) + (mid ? (rng.nextFloat() - 0.5f) * 0.10f : 0f);
            float y = BASE_Y + MathUtils.sin(t * MathUtils.PI) * (0.03f + rng.nextFloat() * 0.07f)
                             + rng.nextFloat() * 0.02f;
            b.pts[i++] = MathUtils.clamp(x, -hx, hx);
            b.pts[i++] = y;
            b.pts[i++] = MathUtils.clamp(z, -hz, hz);
        }
        b.width = 0.013f;
        b.phase = rng.nextFloat() * MathUtils.PI2;
        b.amp = 0.024f;
        b.sustained = true;
        return b;
    }

    /** An arc that leaps off the slab and comes back down — the 3D silhouette of the effect. */
    private Bolt archBolt(float hx, float hz) {
        int n = 12;
        float x0 = rnd(hx * 0.9f), z0 = rnd(hz * 0.9f);
        float ang = rng.nextFloat() * MathUtils.PI2;
        float dist = 0.25f + rng.nextFloat() * 0.55f;
        float x1 = MathUtils.clamp(x0 + MathUtils.cos(ang) * dist, -hx, hx);
        float z1 = MathUtils.clamp(z0 + MathUtils.sin(ang) * dist * 0.8f, -hz, hz);
        float height = 0.10f + rng.nextFloat() * 0.28f;
        Bolt b = new Bolt();
        b.pts = new float[n * 3];
        int i = 0;
        for (int k = 0; k < n; k++) {
            float t = k / (float) (n - 1);
            boolean mid = k > 0 && k < n - 1;
            float x = MathUtils.lerp(x0, x1, t) + (mid ? (rng.nextFloat() - 0.5f) * 0.06f : 0f);
            float z = MathUtils.lerp(z0, z1, t) + (mid ? (rng.nextFloat() - 0.5f) * 0.05f : 0f);
            float y = BASE_Y + MathUtils.sin(t * MathUtils.PI) * height;
            b.pts[i++] = x; b.pts[i++] = y; b.pts[i++] = z;
        }
        b.width = 0.012f;
        b.phase = rng.nextFloat() * MathUtils.PI2;
        b.amp = 0.018f;
        b.sustained = true;
        return b;
    }

    /** Replace transient bolts: 0-2 extra discharges + a couple of tiny crackles. */
    private void rollTransients(List<Bolt> list, float hx, float hz) {
        list.removeIf(b -> !b.sustained);
        int cross = rng.nextInt(2);
        for (int a = 0; a < cross; a++) {
            Bolt b = fieldBolt(hx, hz, rng.nextInt(4));
            b.width = 0.011f;
            b.amp = 0.016f;
            b.sustained = false;
            list.add(b);
        }
        for (int s = 0; s < 2; s++) {
            int n = 3;
            float sx = rnd(hx), sz = rnd(hz);
            Bolt b = new Bolt();
            b.pts = new float[n * 3];
            int i = 0;
            for (int k = 0; k < n; k++) {
                b.pts[i++] = MathUtils.clamp(sx + rnd(0.08f), -hx, hx);
                b.pts[i++] = BASE_Y + rng.nextFloat() * 0.06f;
                b.pts[i++] = MathUtils.clamp(sz + rnd(0.06f), -hz, hz);
            }
            b.width = 0.007f;
            b.phase = rng.nextFloat() * MathUtils.PI2;
            b.amp = 0.012f;
            b.sustained = false;
            list.add(b);
        }
    }

    /** Rebuild this tile's ribbons: rest paths + time-morph displacement, camera-facing. */
    private void rebuild(int col, int row, Camera cam, float time) {
        float tileX = Battlefield.floorX(col), tileZ = Battlefield.floorZ(row);
        int i = 0;
        for (Bolt b : bolts[col][row]) {
            float bright = 0.88f + 0.12f * MathUtils.sin(time * 7f + b.phase * 3f);
            float col4 = Color.toFloatBits(bright, 0.98f * bright, 0.78f * bright, 1f);
            int n = b.pts.length / 3;
            float px = 0, py = 0, pz = 0;
            for (int k = 0; k < n; k++) {
                float rx = b.pts[k * 3], ry = b.pts[k * 3 + 1], rz = b.pts[k * 3 + 2];
                float x = rx + MathUtils.sin(time * 1.9f + b.phase + k * 1.7f) * b.amp;
                float z = rz + MathUtils.cos(time * 1.6f + b.phase * 1.3f + k * 2.3f) * b.amp * 0.8f;
                float y = ry + (0.5f + 0.5f * MathUtils.sin(time * 2.4f + b.phase + k * 2.9f)) * 0.025f;
                if (k > 0 && i + 30 <= scratch.length) {
                    float dx = x - px, dy = y - py, dz = z - pz;
                    float mx = tileX + (px + x) * 0.5f, my = (py + y) * 0.5f, mz = tileZ + (pz + z) * 0.5f;
                    float vx = mx - cam.position.x, vy = my - cam.position.y, vz = mz - cam.position.z;
                    float ox = dy * vz - dz * vy, oy = dz * vx - dx * vz, oz = dx * vy - dy * vx;
                    float ol = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
                    if (ol > 1e-5f) {
                        float s = b.width / ol;
                        ox *= s; oy *= s; oz *= s;
                        i = put(i, px - ox, py - oy, pz - oz, col4, -1f);
                        i = put(i, px + ox, py + oy, pz + oz, col4,  1f);
                        i = put(i, x + ox, y + oy, z + oz, col4,  1f);
                        i = put(i, px - ox, py - oy, pz - oz, col4, -1f);
                        i = put(i, x + ox, y + oy, z + oz, col4,  1f);
                        i = put(i, x - ox, y - oy, z - oz, col4, -1f);
                    }
                }
                px = x; py = y; pz = z;
            }
        }
        counts[col][row] = i / 5;
        meshes[col][row].setVertices(scratch, 0, i);
    }

    private float rnd(float half) { return (rng.nextFloat() * 2f - 1f) * half; }

    private int put(int i, float x, float y, float z, float col, float edge) {
        scratch[i++] = x; scratch[i++] = y; scratch[i++] = z; scratch[i++] = col; scratch[i++] = edge;
        return i;
    }

    @Override public void dispose() {
        for (int c = 0; c < Battlefield.COLS; c++)
            for (int r = 0; r < Battlefield.ROWS; r++)
                tileLost(c, r);
        shader.dispose();
    }
}
