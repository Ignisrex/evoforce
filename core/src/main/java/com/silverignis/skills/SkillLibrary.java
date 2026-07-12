package com.silverignis.skills;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue of every skill the game knows about. Owns the underlying
 * {@link com.badlogic.gdx.graphics.Texture}s for skill icons and VFX —
 * call {@link #dispose()} when the battle ends.
 *
 * <p>This is the global pool. The subset of skills a specific caster can
 * actually draw from lives on their {@code SkillDeck}, populated from the
 * library at battle start (and mutated by in-game systems thereafter).
 */
public class SkillLibrary {

    private final Map<String, Skill> byId = new HashMap<>();
    private final List<Skill> all = new ArrayList<>();

    /**
     * Load the starter pool from {@code assets/skills/skills.json}. Only
     */
    public static SkillLibrary defaults() {
        return SkillLoader.load(Gdx.files.internal("skills/skills.json"));
    }

    public void add(Skill skill) {
        byId.put(skill.getId(), skill);
        all.add(skill);
    }

    public Skill get(String id) {
        return byId.get(id);
    }

    public List<Skill> all() {
        return Collections.unmodifiableList(all);
    }

    public void dispose() {
        for (Skill s : all) {
            if (s.getIcon() != null) s.getIcon().dispose();
            if (s.getVfxTexture() != null) s.getVfxTexture().dispose();
            if (s.getZoneTexture() != null) s.getZoneTexture().dispose();
            if (s.getVfxAnimationSheet() != null) s.getVfxAnimationSheet().dispose();
        }
    }
}
