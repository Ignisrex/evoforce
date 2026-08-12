package com.silverignis.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

public final class CaveTheme {

    public static final long OVERWORLD_SEED = 1337L;

    public final Texture wallTex;
    public final Texture floorTex;
    public final Texture floorEmissiveTex;

    public final int crystalClusters, loneCrystals, hangingCrystals;

    public final Color fogColor;
    public final Color ambient;
    public final Color[] lightPalette;   // colors for the 3 crystal-anchored point lights

    private CaveTheme(Texture wallTex, Texture floorTex, Texture floorEmissiveTex,
                      int crystalClusters, int loneCrystals, int hangingCrystals,
                      Color fogColor, Color ambient, Color[] lightPalette) {
        this.wallTex = wallTex;
        this.floorTex = floorTex;
        this.floorEmissiveTex = floorEmissiveTex;
        this.crystalClusters = crystalClusters;
        this.loneCrystals = loneCrystals;
        this.hangingCrystals = hangingCrystals;
        this.fogColor = fogColor;
        this.ambient = ambient;
        this.lightPalette = lightPalette;
    }

    public static CaveTheme cave(Texture wall, Texture floor, Texture floorEmissive) {
        return new CaveTheme(wall, floor, floorEmissive, 5, 12, 6,
            new Color(0.05f, 0.08f, 0.15f, 1f),           // fog: hazy blue
            new Color(0.22f, 0.22f, 0.32f, 1f),           // ambient
            new Color[] {
                new Color(0.35f, 0.80f, 0.90f, 1f),       // cyan
                new Color(0.85f, 0.45f, 0.75f, 1f),       // pink
                new Color(0.45f, 0.55f, 0.95f, 1f),       // blue-violet
            });
    }
}
