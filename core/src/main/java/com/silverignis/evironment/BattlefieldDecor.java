package com.silverignis.evironment;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.silverignis.entities.Battlefield;

public class BattlefieldDecor {

    private BattlefieldDecor(){}

    public static void apply(GameEnvironment env, Battlefield bf) {
        Material blue = new Material(
            ColorAttribute.createDiffuse(0.18f, 0.22f, 0.50f, 1f),
            ColorAttribute.createSpecular(0.30f, 0.55f, 1.00f, 1f));
        Material red = new Material(
            ColorAttribute.createDiffuse(0.50f, 0.16f, 0.18f, 1f),
            ColorAttribute.createSpecular(1.00f, 0.25f, 0.30f, 1f));

        float gap = 0.04f, panelH = 0.03f, panelY = panelH * 0.5f;
        float w = bf.panelFloorWidth() - gap;
        float d = bf.panelFloorDepth() - gap;
        for (int col = 0; col < Battlefield.COLS; col++) {
            for (int row = 0; row < Battlefield.ROWS; row++) {
                Material mat = bf.isPlayerSide(col) ? blue : red;
                env.addDecor(mat, w, panelH, d, bf.floorX(col), panelY, bf.floorZ(row));
            }
        }
    }
}
