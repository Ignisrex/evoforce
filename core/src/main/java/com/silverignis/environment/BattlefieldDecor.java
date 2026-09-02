package com.silverignis.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.silverignis.entities.Battlefield;

/** Side-colored slabs + skill-coverage glow. Elemental identity is drawn by PanelSurfaces on top. */
public class BattlefieldDecor {

    private static final Color GLOW_COLOR = new Color(0.14f, 0.12f, 0.05f, 1f);
    private static final float DECAY_PER_SEC = 4f;

    private final ModelInstance[][] panels = new ModelInstance[Battlefield.COLS][Battlefield.ROWS];
    private final float[][] glow = new float[Battlefield.COLS][Battlefield.ROWS];

    public BattlefieldDecor(GameEnvironment env, Battlefield battlefield){
        Material blue = new Material(
            ColorAttribute.createDiffuse(0.18f, 0.22f, 0.50f, 1f),
            ColorAttribute.createSpecular(0.30f, 0.55f, 1.00f, 1f),
            ColorAttribute.createEmissive(0f, 0f, 0f, 1f));
        Material red = new Material(
            ColorAttribute.createDiffuse(0.50f, 0.16f, 0.18f, 1f),
            ColorAttribute.createSpecular(1.00f, 0.25f, 0.30f, 1f),
            ColorAttribute.createEmissive(0f, 0f, 0f, 1f));

        float gap = 0.04f, panelH = 0.03f, panelY = panelH * 0.5f;
        float w = battlefield.panelFloorWidth() - gap;
        float d = battlefield.panelFloorDepth() - gap;
        for (int col = 0; col < Battlefield.COLS; col++) {
            for (int row = 0; row < Battlefield.ROWS; row++) {
                Material mat = battlefield.isPlayerSide(col) ? blue : red;
                panels[col][row] = env.addDecor(mat, w, panelH, d, battlefield.floorX(col), panelY, battlefield.floorZ(row));
            }
        }
    }

    public void glow(int col, int row) {
        if (col < 0 || col >= Battlefield.COLS || row < 0 || row >= Battlefield.ROWS) return;
        glow[col][row] = 1f;
    }

    public void update(float delta) {
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                float g = glow[c][r];
                ColorAttribute em = (ColorAttribute)
                    panels[c][r].materials.get(0).get(ColorAttribute.Emissive);
                em.color.set(GLOW_COLOR.r * g, GLOW_COLOR.g * g, GLOW_COLOR.b * g, 1f);
                glow[c][r] = Math.max(0f, g - DECAY_PER_SEC * delta);
            }
        }
    }

    public static void clear(GameEnvironment env) {
        env.clearDecor();
    }
}
