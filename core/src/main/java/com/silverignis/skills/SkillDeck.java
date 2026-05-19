package com.silverignis.skills;

import com.badlogic.gdx.math.MathUtils;
import com.silverignis.skills.slots.SkillSlots;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Per-caster pool of {@link Skill}s the caster has access to, plus the
 * cooldown state for those skills. Owned by a {@code Caster} (one per
 * battle today); future in-game systems will mutate the membership as
 * skills are acquired or removed during a run.
 */
public final class SkillDeck {

    private final Set<Skill> skills = new LinkedHashSet<>();
    private final SkillCooldowns cooldowns = new SkillCooldowns();

    public SkillDeck() {}

    public void add(Skill skill)     { if (skill != null) skills.add(skill); }
    public void remove(Skill skill)  { skills.remove(skill); }
    public boolean contains(Skill s) { return skills.contains(s); }
    public int size()                 { return skills.size(); }
    public Collection<Skill> all()   { return Collections.unmodifiableCollection(skills); }

    // --- Cooldowns (thin facade over the private SkillCooldowns) ---

    public void update(float delta)       { cooldowns.update(delta); }
    public void onUsed(Skill skill)       { cooldowns.onUsed(skill); }
    public boolean isOnCooldown(Skill s)  { return cooldowns.isOnCooldown(s); }
    public float remainingFor(Skill s)    { return cooldowns.remainingFor(s); }
    public void clearCooldowns()          { cooldowns.clear(); }

    /** Skills in the deck not currently on cooldown. */
    public List<Skill> available() {
        List<Skill> out = new ArrayList<>(skills.size());
        for (Skill s : skills) if (!cooldowns.isOnCooldown(s)) out.add(s);
        return out;
    }

    /**
     * Draw up to {@code n} skills that are off-cooldown and not already
     * sitting in {@code slots}. Caller passes slots explicitly so the deck
     * has no back-reference to its owning caster.
     */
    public List<Skill> drawHand(int n, SkillSlots slots) {
        List<Skill> eligible = new ArrayList<>(skills.size());
        for (Skill s : skills) {
            if (cooldowns.isOnCooldown(s)) continue;
            if (slots != null && slots.contains(s)) continue;
            eligible.add(s);
        }
        Collections.shuffle(eligible, new Random(MathUtils.random.nextLong()));
        return eligible.subList(0, Math.min(n, eligible.size()));
    }
}
