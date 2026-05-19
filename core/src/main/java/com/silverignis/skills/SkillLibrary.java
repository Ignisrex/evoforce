package com.silverignis.skills;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.skills.slots.SkillSlots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue of every skill the player owns this run. Owns the underlying
 * {@link com.badlogic.gdx.graphics.Texture}s for skill icons and VFX —
 * call {@link #dispose()} when the battle ends.
 *
 * <p>The "deck" is just {@code all}. {@link #drawHand} is the one
 * non-trivial piece: it builds a hand of skills that are <em>both</em>
 * off cooldown <em>and</em> not currently sitting in a {@link SkillSlots}
 * slot. Anything sitting in a slot is "in the player's pocket" — it
 * shouldn't show up again in the menu until it's fired.
 */
public class SkillLibrary {

    private final Map<String, Skill> byId = new HashMap<>();
    private final List<Skill> all = new ArrayList<>();

    /**
     * Load the starter pool from {@code assets/skills/skills.json}. Only
     * Wind Strike has real behavior today; the others are valid skill data
     * that just happens to map to stub instances, which is fine for testing
     * the menu / slot / cooldown flow.
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

    /**
     * Draw up to {@code n} skills that are eligible for the staging menu:
     * <ul>
     *   <li>not currently on cooldown, and</li>
     *   <li>not currently sitting in any X/Y/B slot.</li>
     * </ul>
     * If fewer than {@code n} are eligible, returns however many there are.
     */
    public List<Skill> drawHand(int n, SkillCooldowns cooldowns, SkillSlots slots) {
        List<Skill> eligible = new ArrayList<>();
        for (Skill s : all) {
            if (cooldowns.isOnCooldown(s)) continue;
            if (slots != null && slots.contains(s)) continue;
            eligible.add(s);
        }
        Collections.shuffle(eligible, new java.util.Random(MathUtils.random.nextLong()));
        return eligible.subList(0, Math.min(n, eligible.size()));
    }

    public void dispose() {
        for (Skill s : all) {
            if (s.getIcon() != null) s.getIcon().dispose();
            if (s.getVfxTexture() != null) s.getVfxTexture().dispose();
            if (s.getVfxAnimationSheet() != null) s.getVfxAnimationSheet().dispose();
        }
    }
}
