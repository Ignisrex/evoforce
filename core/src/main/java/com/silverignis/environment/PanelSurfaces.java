package com.silverignis.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.silverignis.assets.GameAssets;
import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;

import java.util.EnumMap;

/**
 * Host of the elemental panel pass, drawn inside the 3D window — after
 * GameEnvironment.render, before the depth clear — so surfaces are depth-correct
 * against slabs and cave geometry, and billboards still land on top.
 *
 * Owns the shared tile quad, the per-frame tile diff (fires tileGained/tileLost
 * on each {@link PanelSurface}), and the common uniform contract: u_projTrans +
 * u_time always; u_camPos, u_star (effect sprite), u_side (team color) when a
 * shader declares them. Holds GameAssets and the ParticleEngine so surfaces can
 * use effect sprites and ambient particles.
 */
public class PanelSurfaces implements Disposable {

    private static final int SEG_X = 16, SEG_Z = 8;
    private static final float INSET = 0.18f;   // leaves a thin band of the original slab framing the surface

    private static final float[] SIDE_BLUE = {0.18f, 0.22f, 0.50f};
    private static final float[] SIDE_RED  = {0.50f, 0.16f, 0.18f};

    private final Battlefield battlefield;
    private final GameAssets assets;
    private final ParticleEngine particles;
    private final Texture star;
    private final Mesh grid;
    private final float halfW, halfD;
    private final EnumMap<Battlefield.PanelType, PanelSurface> surfaces =
        new EnumMap<>(Battlefield.PanelType.class);
    private final Battlefield.PanelType[][] shown =
        new Battlefield.PanelType[Battlefield.COLS][Battlefield.ROWS];
    private float time;

    public PanelSurfaces(Battlefield battlefield, GameAssets assets, ParticleEngine particles) {
        this.battlefield = battlefield;
        this.assets = assets;
        this.particles = particles;
        this.star = assets.star(1);
        this.grid = buildGrid();
        this.halfW = (battlefield.panelFloorWidth() - INSET) * 0.5f;
        this.halfD = (battlefield.panelFloorDepth() - INSET) * 0.5f;

        add(new FirePanelSurface());
        add(new IcePanelSurface());
        add(new LightningPanelSurface());
        add(new ShaderPanelSurface(Battlefield.PanelType.DARK, "dark"));
        add(new ShaderPanelSurface(Battlefield.PanelType.POISON, "poison"));
        add(new NaturePanelSurface());
    }

    private void add(PanelSurface s) {
        s.bind(this);
        surfaces.put(s.type(), s);
    }

    /** Unit quad in [-0.5,0.5]², subdivided so vertex shaders can displace it. */
    private static Mesh buildGrid() {
        int vx = SEG_X + 1, vz = SEG_Z + 1;
        float[] verts = new float[vx * vz * 2];
        int i = 0;
        for (int z = 0; z < vz; z++)
            for (int x = 0; x < vx; x++) {
                verts[i++] = x / (float) SEG_X - 0.5f;
                verts[i++] = z / (float) SEG_Z - 0.5f;
            }
        short[] idx = new short[SEG_X * SEG_Z * 6];
        int k = 0;
        for (int z = 0; z < SEG_Z; z++)
            for (int x = 0; x < SEG_X; x++) {
                int a = z * vx + x, b = a + 1, c = a + vx, d = c + 1;
                idx[k++] = (short) a; idx[k++] = (short) c; idx[k++] = (short) b;
                idx[k++] = (short) b; idx[k++] = (short) c; idx[k++] = (short) d;
            }
        Mesh m = new Mesh(true, vx * vz, idx.length,
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));
        m.setVertices(verts);
        m.setIndices(idx);
        return m;
    }

    /** Call between environment.render(...) and the depth-buffer clear. */
    public void render(Camera cam) {
        time += Gdx.graphics.getDeltaTime();
        syncTiles();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);   // grass blades face every which way
        for (PanelSurface s : surfaces.values()) s.render(cam);
    }

    /** Fires tileGained/tileLost on the surfaces as panel state changes (initial layout included). */
    private void syncTiles() {
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                Battlefield.PanelType now = battlefield.getPanel(c, r);
                if (now == shown[c][r]) continue;
                PanelSurface old = surfaces.get(shown[c][r]);
                if (old != null) old.tileLost(c, r);
                PanelSurface s = surfaces.get(now);
                if (s != null) s.tileGained(c, r);
                shown[c][r] = now;
            }
        }
    }

    // ── surface-facing API ───────────────────────────────────────────────

    Battlefield battlefield()   { return battlefield; }
    GameAssets assets()         { return assets; }
    ParticleEngine particles()  { return particles; }
    float time()                { return time; }
    float halfW()               { return halfW; }
    float halfD()               { return halfD; }

    /** u_projTrans + guarded u_time / u_camPos / u_star. Call once after shader.bind(). */
    void commonUniforms(ShaderProgram sh, Camera cam) {
        sh.setUniformMatrix("u_projTrans", cam.combined);
        if (sh.hasUniform("u_time"))   sh.setUniformf("u_time", time);
        if (sh.hasUniform("u_camPos")) sh.setUniformf("u_camPos", cam.position);
        if (sh.hasUniform("u_star"))   { star.bind(0); sh.setUniformi("u_star", 0); }
    }

    /** Sets per-tile u_tile (+ u_side team color when declared) and draws the shared quad. */
    void drawTileQuad(ShaderProgram sh, int c, int r) {
        if (sh.hasUniform("u_side")) {
            float[] side = battlefield.isPlayerSide(c) ? SIDE_BLUE : SIDE_RED;
            sh.setUniformf("u_side", side[0], side[1], side[2]);
        }
        sh.setUniformf("u_tile", Battlefield.floorX(c), Battlefield.floorZ(r), halfW, halfD);
        grid.render(sh, GL20.GL_TRIANGLES);
    }

    @Override public void dispose() {
        for (PanelSurface s : surfaces.values()) s.dispose();
        grid.dispose();
    }
}
