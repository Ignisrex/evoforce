package com.silverignis.traits;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue of every trait the game knows about. Owns the icon
 * {@link com.badlogic.gdx.graphics.Texture}s — call {@link #dispose()}.
 * The subset a caster owns lives on their {@code TraitsContainer}.
 */
public class TraitLibrary {

    private final Map<String, Trait> byId = new HashMap<>();
    private final List<Trait> all = new ArrayList<>();

    public static TraitLibrary defaults() {
        return TraitLoader.load(Gdx.files.internal("traits/traits.json"));
    }

    public void add(Trait trait) {
        byId.put(trait.getId(), trait);
        all.add(trait);
    }

    public Trait get(String id) {
        return byId.get(id);
    }

    public List<Trait> all() {
        return Collections.unmodifiableList(all);
    }

    public void dispose() {
        for (Trait t : all) t.getIcon().dispose();
    }
}
