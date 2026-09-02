package com.silverignis.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.entities.Battlefield;

import java.util.Random;

/**
 * FIRE: the lava surface stays as the ground; the panel burns with a few large
 * and several small camera-facing flame cards — flame-proportioned quads
 * billboarded around their vertical axis in the vertex shader, each running a
 * teardrop erosion frag (upward-scrolling noise pinches licks off, hottest at
 * the base core). Additive; bloom feeds on the cores.
 */
public class FirePanelSurface extends ShaderPanelSurface {

    private static final int BIG = 4, SMALL = 6;
    private static final float MARGIN = 0.14f;
    private static final float BASE_Y = 0.034f;   // just above the lava quad

    private final ShaderProgram flame;
    private final Mesh[][] flames = new Mesh[Battlefield.COLS][Battlefield.ROWS];

    public FirePanelSurface() {
        super(Battlefield.PanelType.FIRE, "fire");
        this.flame = load("flame");
    }

    @Override public void tileGained(int col, int row) {
        flames[col][row] = buildFlames(col * 37 + row * 11);
    }

    @Override public void tileLost(int col, int row) {
        if (flames[col][row] != null) { flames[col][row].dispose(); flames[col][row] = null; }
    }

    @Override public void render(Camera cam) {
        super.render(cam);   // lava ground

        Battlefield bf = host.battlefield();
        // horizontal camera-right for the cylindrical billboards
        float rx = -cam.direction.z, rz = cam.direction.x;
        float rl = (float) Math.sqrt(rx * rx + rz * rz);
        if (rl < 1e-5f) { rx = 1f; rz = 0f; } else { rx /= rl; rz /= rl; }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
        Gdx.gl.glDepthMask(false);
        boolean bound = false;
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                if (bf.getPanel(c, r) != type || flames[c][r] == null) continue;
                if (!bound) {
                    flame.bind();
                    host.commonUniforms(flame, cam);
                    flame.setUniformf("u_camRight", rx, rz);
                    bound = true;
                }
                flame.setUniformf("u_tileCenter", Battlefield.floorX(c), Battlefield.floorZ(r));
                flames[c][r].render(flame, GL20.GL_TRIANGLES);
            }
        }
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Static quads (billboarding happens in the vertex shader): base + corner + size + phase/heat. */
    private Mesh buildFlames(int seed) {
        Random rng = new Random(seed);
        float hx = host.halfW() - MARGIN, hz = host.halfD() - MARGIN;
        int total = BIG + SMALL;
        float[] v = new float[total * 6 * 9];   // 6 verts/card, 9 floats/vert
        int i = 0;
        for (int f = 0; f < total; f++) {
            boolean big = f < BIG;
            float bx = (rng.nextFloat() * 2f - 1f) * hx;
            float bz = (rng.nextFloat() * 2f - 1f) * hz;
            float h  = big ? 0.30f + rng.nextFloat() * 0.18f : 0.14f + rng.nextFloat() * 0.10f;
            float hw = big ? 0.13f + rng.nextFloat() * 0.07f : 0.07f + rng.nextFloat() * 0.04f;
            float phase = rng.nextFloat() * MathUtils.PI2;
            float heat = rng.nextFloat();
            // two triangles of one quad: corners (u, v)
            i = vert(v, i, bx, bz, -1f, 0f, hw, h, phase, heat);
            i = vert(v, i, bx, bz,  1f, 0f, hw, h, phase, heat);
            i = vert(v, i, bx, bz,  1f, 1f, hw, h, phase, heat);
            i = vert(v, i, bx, bz, -1f, 0f, hw, h, phase, heat);
            i = vert(v, i, bx, bz,  1f, 1f, hw, h, phase, heat);
            i = vert(v, i, bx, bz, -1f, 1f, hw, h, phase, heat);
        }
        Mesh m = new Mesh(true, total * 6, 0,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_base"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_corner"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_size"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_fx"));
        m.setVertices(v);
        return m;
    }

    private static int vert(float[] v, int i, float bx, float bz,
                            float u, float vy, float hw, float h, float phase, float heat) {
        v[i++] = bx; v[i++] = BASE_Y; v[i++] = bz;
        v[i++] = u; v[i++] = vy;
        v[i++] = hw; v[i++] = h;
        v[i++] = phase; v[i++] = heat;
        return i;
    }

    @Override public void dispose() {
        for (int c = 0; c < Battlefield.COLS; c++)
            for (int r = 0; r < Battlefield.ROWS; r++)
                tileLost(c, r);
        flame.dispose();
        super.dispose();
    }
}
