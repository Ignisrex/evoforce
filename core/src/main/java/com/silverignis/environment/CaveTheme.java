package com.silverignis.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ObjectMap;

public final class CaveTheme {

    public static final long OVERWORLD_SEED = 1337L;

    /** Colour for one arena material slot; keyed by the Blender material name.
     *  The model ships flat placeholder colours — this is the only colour authority. */
    public record Slot(Color diffuse, Color emissive, Texture texture) {
        static Slot of(float r, float g, float b, float er, float eg, float eb) {
            return new Slot(new Color(r, g, b, 1f), new Color(er, eg, eb, 1f), null);
        }
        static Slot textured(Texture tex) {
            return new Slot(Color.WHITE, new Color(0f, 0f, 0f, 1f), tex);
        }
    }

    public final Texture wallTex;
    public final Texture floorTex;
    public final Texture floorEmissiveTex;

    public final int crystalClusters, loneCrystals, hangingCrystals;

    public final Color fogColor;
    public final Color ambient;
    public final Color[] lightPalette;   // colors for the 3 crystal-anchored point lights

    public final ObjectMap<String, Slot> palette;   // authored arena: slot name -> colour

    private CaveTheme(Texture wallTex, Texture floorTex, Texture floorEmissiveTex,
                      int crystalClusters, int loneCrystals, int hangingCrystals,
                      Color fogColor, Color ambient, Color[] lightPalette,
                      ObjectMap<String, Slot> palette) {
        this.wallTex = wallTex;
        this.floorTex = floorTex;
        this.floorEmissiveTex = floorEmissiveTex;
        this.crystalClusters = crystalClusters;
        this.loneCrystals = loneCrystals;
        this.hangingCrystals = hangingCrystals;
        this.fogColor = fogColor;
        this.ambient = ambient;
        this.lightPalette = lightPalette;
        this.palette = palette;
    }

    public static CaveTheme cave(Texture wall, Texture floor, Texture floorEmissive) {
        // Slot colours picked from the concept art; the floor reuses the tiled cave texture.
        ObjectMap<String, Slot> palette = new ObjectMap<>();
        palette.put("MAT_stone_deep",  Slot.of(0.06f, 0.06f, 0.16f,  0f, 0f, 0f));   // ceiling, recesses
        palette.put("MAT_stone_dark",  Slot.of(0.13f, 0.12f, 0.30f,  0f, 0f, 0f));   // rim, walls
        palette.put("MAT_stone_floor", Slot.textured(floor));                           // flagstones
        palette.put("MAT_stone_light", Slot.of(0.30f, 0.31f, 0.52f,  0f, 0f, 0f));   // lit lips
        palette.put("MAT_crystal",     Slot.of(0.20f, 0.45f, 0.95f,  0.55f, 0.80f, 1.00f));  // cores
        palette.put("MAT_crystal_dim", Slot.of(0.15f, 0.30f, 0.85f,  0.10f, 0.35f, 0.95f));  // skirts
        palette.put("MAT_water",       Slot.of(0.35f, 0.65f, 0.95f,  0.30f, 0.55f, 0.80f));

        return new CaveTheme(wall, floor, floorEmissive, 5, 12, 6,
            new Color(0.05f, 0.08f, 0.15f, 1f),           // fog: hazy blue
            new Color(0.22f, 0.22f, 0.32f, 1f),           // ambient
            new Color[] {
                new Color(0.35f, 0.80f, 0.90f, 1f),       // cyan
                new Color(0.85f, 0.45f, 0.75f, 1f),       // pink
                new Color(0.45f, 0.55f, 0.95f, 1f),       // blue-violet
            },
            palette);
    }
}
