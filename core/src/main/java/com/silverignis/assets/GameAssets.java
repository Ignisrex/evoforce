package com.silverignis.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.silverignis.animation.AnimSet;
import com.silverignis.animation.AnimSheet;
import com.silverignis.animation.AnimState;
import com.silverignis.animation.FrameClip;
import com.silverignis.components.Team;
import com.silverignis.registry.Monster;

import java.util.EnumMap;

/**
 * Central owner of all file-loaded textures and audio, wrapping one libGDX
 * {@link AssetManager}. Screens borrow assets through the typed facade and must
 * NOT dispose them individually — {@link #dispose()} frees everything at once.
 *
 * <p>Three disjoint asset sets exist in the game; this class owns one of them:
 * <ul>
 *   <li>Non-monster files (cave, clash VFX, overworld avatar, audio) — owned here.</li>
 *   <li>Monster directional sprites — also loaded here ({@link #queueLoad()} iterates the
 *       {@code Monster} enum); they are <em>fetched</em> through {@code MonsterRegistry},
 *       which holds this {@code GameAssets}. One cache, one dispose.</li>
 *   <li>Skill icons / VFX textures — owned and disposed by {@code SkillLibrary}.</li>
 * </ul>
 * Because the sets are disjoint, nothing is owned twice and there is no double-dispose.
 * Do not register skill or generated (Pixmap) textures with this manager.
 */
public final class GameAssets implements Disposable {

    public static final String CAVE_WALL  = "cave_wall.png";
    public static final String CAVE_FLOOR = "cave_floor.png";
    // Effect texture sets, individually addressable (spark/star/smoke/circle/flare(int), 1-based) —
    // effects pick which frames and in what order at their build site.
    public static final String[] SPARK_SET = {
        "effects/spark_01.png", "effects/spark_02.png", "effects/spark_03.png",
        "effects/spark_04.png", "effects/spark_05.png", "effects/spark_06.png",
        "effects/spark_07.png",
    };

    public static final String[] STAR_SET = {
        "effects/star_01.png", "effects/star_02.png", "effects/star_03.png",
        "effects/star_04.png", "effects/star_05.png", "effects/star_06.png",
        "effects/star_07.png", "effects/star_08.png", "effects/star_09.png",
    };

    public static final String[] SMOKE_SET = {
        "effects/smoke_01.png", "effects/smoke_02.png", "effects/smoke_03.png",
        "effects/smoke_04.png", "effects/smoke_05.png", "effects/smoke_06.png",
        "effects/smoke_07.png", "effects/smoke_08.png", "effects/smoke_09.png",
        "effects/smoke_10.png",
    };

    public static final String[] CIRCLE_SET = {
        "effects/circle_01.png", "effects/circle_02.png", "effects/circle_03.png",
        "effects/circle_04.png", "effects/circle_05.png",
    };

    public static final String[] FLARE_SET = {
        "effects/flare_01.png",
    };

    public static final String[] FLAME_SET = {
        "effects/flame_01.png", "effects/flame_02.png", "effects/flame_03.png",
        "effects/flame_04.png", "effects/flame_05.png", "effects/flame_06.png",
    };

    public static final String[] MAGIC_SET = {
        "effects/magic_01.png", "effects/magic_02.png", "effects/magic_03.png",
        "effects/magic_04.png", "effects/magic_05.png",
    };

    public static final String[] LIGHT_SET = {
        "effects/light_01.png", "effects/light_02.png", "effects/light_03.png",
    };

    public static final String[] SYMBOL_SET = {
        "effects/symbol_01.png", "effects/symbol_02.png",
    };

    public static final String[] TWIRL_SET = {
        "effects/twirl_01.png", "effects/twirl_02.png", "effects/twirl_03.png",
    };

    public static final String[] EFFECT_A_SET = {
        "effects/effect_01_a.png", "effects/effect_02_a.png", "effects/effect_03_a.png",
    };

    public static final String[] STAR_A_SET = {
        "effects/star_01_a.png", "effects/star_02_a.png", "effects/star_03_a.png",
        "effects/star_04_a.png", "effects/star_05_a.png", "effects/star_06_a.png",
        "effects/star_07_a.png", "effects/star_08_a.png", "effects/star_09_a.png",
    };

    public static final String[] SPOTLIGHT_A_SET = {
        "effects/spotlight_01_a.png", "effects/spotlight_02_a.png", "effects/spotlight_03_a.png",
        "effects/spotlight_04_a.png", "effects/spotlight_05_a.png", "effects/spotlight_06_a.png",
        "effects/spotlight_07_a.png", "effects/spotlight_08_a.png",
    };

    public static final String[] TRACE_A_SET = {
        "effects/trace_01_a.png", "effects/trace_02_a.png", "effects/trace_03_a.png",
        "effects/trace_04_a.png", "effects/trace_05_a.png", "effects/trace_06_a.png",
        "effects/trace_07_a.png",
    };

    private static final String[][] EFFECT_SETS = {
        SPARK_SET, STAR_SET, SMOKE_SET, CIRCLE_SET, FLARE_SET,
        FLAME_SET, MAGIC_SET, LIGHT_SET, SYMBOL_SET, TWIRL_SET,
        EFFECT_A_SET, STAR_A_SET, SPOTLIGHT_A_SET, TRACE_A_SET,
    };


    private final AssetManager mgr = new AssetManager();
    private final EnumMap<Monster, EnumMap<Team, AnimSet>> animSets = new EnumMap<>(Monster.class);
    private TextureRegion avatarRegion;

    public void queueLoad() {
        mgr.load(CAVE_WALL,  Texture.class);
        // Floor tiles 6x across the cave at perspective — without mipmaps the far rows shimmer.
        TextureLoader.TextureParameter floorParams = new TextureLoader.TextureParameter();
        floorParams.genMipMaps = true;
        mgr.load(CAVE_FLOOR, Texture.class, floorParams);
        for (String[] set : EFFECT_SETS)
            for (String s : set) mgr.load(s, Texture.class);

        for (Monster m : Monster.values()) {
            for (AnimSheet.Row r : m.animSheet().rows()) {
                mgr.load(m.texturePath(r.state, Team.PLAYER), Texture.class);
                mgr.load(m.texturePath(r.state, Team.ENEMY),  Texture.class);
            }
        }
    }

    /**
     * Block until everything queued (here and on the shared manager) is resident, then apply
     * filtering. Synchronous for now; swap for an {@code update()}/{@code getProgress()} loop
     * behind a loading screen later.
     */
    public void finishLoading() {
        mgr.finishLoading();
        caveWall().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        caveFloor().setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        caveFloor().setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        for (String[] set : EFFECT_SETS)
            for (String s : set) texture(s).setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        for (Monster m: Monster.values()) {
            EnumMap<Team, AnimSet> byFacing = new EnumMap<>(Team.class);
            byFacing.put(Team.PLAYER, sliceAnimSet(m, Team.PLAYER));
            byFacing.put(Team.ENEMY,  sliceAnimSet(m, Team.ENEMY));
            animSets.put(m, byFacing);
        }
        avatarRegion = animSet(Monster.BEASTKIN, Team.PLAYER).get(AnimState.IDLE).frame(0f);
    }

    /**
     * Build one facing's {@link AnimSet} from the monster's per-state split sheets.
     * Each state ships its own single-row horizontal strip, so the cell size is the
     * sheet height and frames advance left-to-right.
     */
    public AnimSet sliceAnimSet(Monster m, Team facing) {
        FrameClip idle = null;
        AnimSet pending = null;
        for (AnimSheet.Row r : m.animSheet().rows()) {
            Texture sheet = mgr.get(m.texturePath(r.state, facing), Texture.class);
            int cell = sheet.getHeight(); // single-row strip ⇒ square cell = sheet height
            Array<TextureRegion> frames = new Array<>(r.frameCount);

            for (int i= 0; i < r.frameCount; i++){
                frames.add(new TextureRegion(sheet, i * cell, 0, cell, cell));
            }

            FrameClip clip = new FrameClip(frames, r.fps, r.loop);
            if (r.state == AnimState.IDLE) {
                idle = clip;
                pending = new AnimSet(clip);
            }else if(pending != null) {
                pending.put(r.state, clip);
            }
        }

        if (idle == null) {
            throw new IllegalStateException("Monster " +m+ " AnimSheet must declare IDLE");
        }
        return pending;
    }

    public Texture texture(String path) { return mgr.get(path, Texture.class); }
    public Sound   sound(String path)   { return mgr.get(path, Sound.class); }
    public Music   music(String path)   { return mgr.get(path, Music.class); }

    public Texture caveWall()  { return texture(CAVE_WALL); }
    public Texture caveFloor() { return texture(CAVE_FLOOR); }
    // 1-based, matching the effects/<set>_XX filenames.
    public Texture spark(int n)  { return texture(SPARK_SET[n - 1]); }
    public Texture star(int n)   { return texture(STAR_SET[n - 1]); }
    public Texture smoke(int n)  { return texture(SMOKE_SET[n - 1]); }
    public Texture circle(int n) { return texture(CIRCLE_SET[n - 1]); }
    public Texture flare(int n)  { return texture(FLARE_SET[n - 1]); }
    public Texture flame(int n)  { return texture(FLAME_SET[n - 1]); }
    public Texture magic(int n)  { return texture(MAGIC_SET[n - 1]); }
    public Texture light(int n)  { return texture(LIGHT_SET[n - 1]); }
    public Texture symbol(int n) { return texture(SYMBOL_SET[n - 1]); }
    public Texture twirl(int n)  { return texture(TWIRL_SET[n - 1]); }
    public Texture effectA(int n) { return texture(EFFECT_A_SET[n - 1]); }
    public Texture starA(int n)   { return texture(STAR_A_SET[n - 1]); }
    public Texture spotlightA(int n) { return texture(SPOTLIGHT_A_SET[n - 1]); }
    public Texture traceA(int n)     { return texture(TRACE_A_SET[n - 1]); }
    public TextureRegion avatar()    { return avatarRegion; }

    public AnimSet animSet(Monster m, Team facing) { return animSets.get(m).get(facing); }

    @Override
    public void dispose() {
        mgr.dispose();
    }
}
