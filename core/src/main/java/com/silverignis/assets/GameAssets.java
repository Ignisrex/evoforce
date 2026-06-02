package com.silverignis.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.silverignis.components.Team;
import com.silverignis.registry.Monster;

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
    public static final String AVATAR     = "sprites/beastkin.png";

    private final AssetManager mgr = new AssetManager();

    public void queueLoad() {
        mgr.load(CAVE_WALL,  Texture.class);
        // Floor tiles 6x across the cave at perspective — without mipmaps the far rows shimmer.
        TextureLoader.TextureParameter floorParams = new TextureLoader.TextureParameter();
        floorParams.genMipMaps = true;
        mgr.load(CAVE_FLOOR, Texture.class, floorParams);
        mgr.load(CLASH,      Texture.class);
        mgr.load(AVATAR,     Texture.class);

        for (Monster m : Monster.values()) {
            mgr.load(m.texturePath(Team.PLAYER), Texture.class);
            mgr.load(m.texturePath(Team.ENEMY),  Texture.class);
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
    }

    public Texture texture(String path) { return mgr.get(path, Texture.class); }
    public Sound   sound(String path)   { return mgr.get(path, Sound.class); }
    public Music   music(String path)   { return mgr.get(path, Music.class); }

    public Texture caveWall()  { return texture(CAVE_WALL); }
    public Texture caveFloor() { return texture(CAVE_FLOOR); }
    public Texture clash()     { return texture(CLASH); }
    public Texture avatar()    { return texture(AVATAR); }

    @Override
    public void dispose() {
        mgr.dispose();
    }
}
