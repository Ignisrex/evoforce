package com.silverignis.environment;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.entities.Battlefield;

import java.util.Random;

/**
 * NATURE: mossy ground quad plus flat-shaded low-poly grass blades swaying above
 * it — same faceted language as the cave crystals, no textures. Each nature tile
 * gets its own seeded tuft mesh (stable per battle), built on tileGained and
 * disposed on tileLost.
 */
public class NaturePanelSurface extends PanelSurface {

    private static final int BLADES = 80;
    private static final float MARGIN = 0.08f;              // blades stay on the surface, off the slab rim
    private static final float MIN_H = 0.15f, MAX_H = 0.40f;
    private static final float BASE_Y = 0.03f;              // slab top

    private final ShaderProgram ground;
    private final ShaderProgram grass;
    private final Mesh[][] tufts = new Mesh[Battlefield.COLS][Battlefield.ROWS];

    public NaturePanelSurface() {
        super(Battlefield.PanelType.NATURE);
        this.ground = ShaderPanelSurface.load("nature");
        this.grass  = ShaderPanelSurface.load("grass");
    }

    @Override public void tileGained(int col, int row) {
        tufts[col][row] = buildTuft(col * 31 + row * 7);
    }

    @Override public void tileLost(int col, int row) {
        if (tufts[col][row] != null) { tufts[col][row].dispose(); tufts[col][row] = null; }
    }

    @Override public void render(Camera cam) {
        Battlefield bf = host.battlefield();

        boolean bound = false;
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                if (bf.getPanel(c, r) != type) continue;
                if (!bound) {
                    ground.bind();
                    host.commonUniforms(ground, cam);
                    bound = true;
                }
                host.drawTileQuad(ground, c, r);
            }
        }

        bound = false;
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                if (bf.getPanel(c, r) != type || tufts[c][r] == null) continue;
                if (!bound) {
                    grass.bind();
                    host.commonUniforms(grass, cam);
                    bound = true;
                }
                grass.setUniformf("u_tileCenter", Battlefield.floorX(c), Battlefield.floorZ(r));
                tufts[c][r].render(grass, GL20.GL_TRIANGLES);
            }
        }
    }

    /** Blades in tile-local coords; two crossed triangles each so no view angle loses them. */
    private Mesh buildTuft(int seed) {
        Random rng = new Random(seed);
        float hx = host.halfW() - MARGIN, hz = host.halfD() - MARGIN;
        float[] v = new float[BLADES * 6 * 6];   // 6 verts/blade, 6 floats/vert
        int i = 0;
        for (int b = 0; b < BLADES; b++) {
            float bx = (rng.nextFloat() * 2f - 1f) * hx;
            float bz = (rng.nextFloat() * 2f - 1f) * hz;
            float h  = MIN_H + rng.nextFloat() * (MAX_H - MIN_H);
            float hw = 0.018f + rng.nextFloat() * 0.016f;
            float yaw = rng.nextFloat() * MathUtils.PI2;
            float phase = rng.nextFloat() * MathUtils.PI2;
            float leanX = (rng.nextFloat() - 0.5f) * 0.08f;
            float leanZ = (rng.nextFloat() - 0.5f) * 0.08f;
            float g = 0.32f + rng.nextFloat() * 0.20f;
            float baseCol = Color.toFloatBits(0.05f, g * 0.55f, 0.05f, 1f);
            float tipCol  = Color.toFloatBits(0.22f, Math.min(1f, g + 0.35f), 0.16f, 1f);
            for (int t = 0; t < 2; t++) {
                float a = yaw + t * MathUtils.HALF_PI;
                float dx = MathUtils.cos(a) * hw, dz = MathUtils.sin(a) * hw;
                i = vert(v, i, bx - dx, BASE_Y, bz - dz, baseCol, phase, 0f);
                i = vert(v, i, bx + dx, BASE_Y, bz + dz, baseCol, phase, 0f);
                i = vert(v, i, bx + leanX, BASE_Y + h, bz + leanZ, tipCol, phase, 1f);
            }
        }
        Mesh m = new Mesh(true, BLADES * 6, 0,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_sway"));
        m.setVertices(v);
        return m;
    }

    private static int vert(float[] v, int i, float x, float y, float z, float col, float phase, float weight) {
        v[i++] = x; v[i++] = y; v[i++] = z; v[i++] = col; v[i++] = phase; v[i++] = weight;
        return i;
    }

    @Override public void dispose() {
        for (int c = 0; c < Battlefield.COLS; c++)
            for (int r = 0; r < Battlefield.ROWS; r++)
                tileLost(c, r);
        ground.dispose();
        grass.dispose();
    }
}
