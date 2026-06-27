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
    public static final String CLASH      = "effects/clash.png";


    private final AssetManager mgr = new AssetManager();
    private final EnumMap<Monster, EnumMap<Team, AnimSet>> animSets = new EnumMap<>(Monster.class);
    private TextureRegion avatarRegion;

    public void queueLoad() {
        mgr.load(CAVE_WALL,  Texture.class);
        // Floor tiles 6x across the cave at perspective — without mipmaps the far rows shimmer.
        TextureLoader.TextureParameter floorParams = new TextureLoader.TextureParameter();
        floorParams.genMipMaps = true;
        mgr.load(CAVE_FLOOR, Texture.class, floorParams);
        mgr.load(CLASH,      Texture.class);

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
    public Texture clash()     { return texture(CLASH); }
    public TextureRegion avatar()    { return avatarRegion; }

    public AnimSet animSet(Monster m, Team facing) { return animSets.get(m).get(facing); }

    @Override
    public void dispose() {
        mgr.dispose();
    }
}
