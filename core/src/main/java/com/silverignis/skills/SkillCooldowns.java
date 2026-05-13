package com.silverignis.skills;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-skill cooldown timers. A skill on cooldown is still in the deck
 * conceptually, but is excluded from {@link SkillLibrary#drawHand} until
 * its timer hits zero.
 *
 * <p>Owned by the {@code GameScreen} (one per battle) and ticked from
 * {@code PlayState.update()}.
 */
public class SkillCooldowns {

    private final Map<String, Float> remaining = new HashMap<>();

    /** Decrement every active timer by {@code delta}; remove finished ones. */
    public void update(float delta) {
        if (remaining.isEmpty()) return;
        // Iterate via the entry set so we can remove during traversal.
        java.util.Iterator<Map.Entry<String, Float>> it = remaining.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> e = it.next();
            float t = e.getValue() - delta;
            if (t <= 0f) it.remove();
            else         e.setValue(t);
        }
    }

    /** Start (or restart) a cooldown for the given skill. */
    public void onUsed(Skill skill) {
        if (skill.getCooldown() > 0f) {
            remaining.put(skill.getId(), skill.getCooldown());
        }
    }

    public boolean isOnCooldown(Skill skill) {
        return remaining.containsKey(skill.getId());
    }

    /** 0..N seconds remaining. Useful for HUD readouts. */
    public float remainingFor(Skill skill) {
        Float t = remaining.get(skill.getId());
        return t == null ? 0f : t;
    }

    public void clear() {
        remaining.clear();
    }
}
