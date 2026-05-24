package com.silverignis.registry;

import com.badlogic.gdx.graphics.Texture;
import com.silverignis.assets.GameAssets;
import com.silverignis.components.Team;

/**
 * Fetch facade for monster sprites. The textures are loaded and owned by
 * {@link GameAssets} (its {@code queueLoad} iterates the {@link Monster} enum);
 * this registry just resolves a (monster, team) pair to the right facing and
 * pulls it from {@code GameAssets}. The path convention lives on {@link Monster}.
 */
public class MonsterRegistry {

    private final GameAssets assets;

    public MonsterRegistry(GameAssets assets) {
        this.assets = assets;
    }

    public Texture getMonsterTexture(Monster monster, Team team) {
        return assets.texture(monster.texturePath(team));
    }
}
