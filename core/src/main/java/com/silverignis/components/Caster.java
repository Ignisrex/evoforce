package com.silverignis.components;

import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.skills.slots.SlotKey;
import com.silverignis.traits.TraitsContainer;
import com.silverignis.util.InputLock;

import java.util.ArrayList;
import java.util.List;

/**
 * Role component for entities that can stage and cast skills. Owns:
 * <ul>
 *   <li>{@link SkillDeck} — skills the caster has access to + cooldown timers;</li>
 *   <li>{@link SkillSlots} — the X/Y/B staged loadout;</li>
 *   <li>a swappable {@code basicAttack} {@link Skill} — fired by the dedicated
 *       {@code ATTACK_BASIC} button;</li>
 *   <li>the caster's {@link Team} — read by team-aware skill instances (notably
 *       {@code ProjectileInstance}) to flip direction and pick the opposing target;</li>
 *   <li>an {@link InputLock} — held by skill instances during dash/cast phases
 *       to gate movement and further input on the entity.</li>
 * </ul>
 *
 * <p>Composed onto an entity (rather than inherited from). The entity's own
 * {@code update} must call {@link #update(float)} so deck cooldowns tick.
 */
public class Caster {

    private final Team       team;
    private final SkillDeck  deck      = new SkillDeck();
    private final SkillSlots slots     = new SkillSlots();
    private final InputLock  inputLock = new InputLock();

    private Skill basicAttack;

    public record LoadedSkill(Skill skill, SlotKey from) { }

    private final List<LoadedSkill> loadedSkills = new ArrayList<>();

    private final TraitsContainer traits = new TraitsContainer();

    public Caster(Team team) {
        this.team = team;
    }

    public Team       getTeam()             { return team; }
    public SkillDeck  getDeck()             { return deck; }
    public SkillSlots getSlots()            { return slots; }
    public InputLock  getInputLock()        { return inputLock; }
    public Skill      getBasicAttack()      { return basicAttack; }
    public void       setBasicAttack(Skill s) { this.basicAttack = s; }

    public TraitsContainer getTraits() { return this.traits; }

    /** Tick per-caster state (currently just cooldowns inside the deck). */
    public void update(float delta) {
        deck.update(delta);
    }

    public void loadSkill(Skill skill, SlotKey from) {
        this.loadedSkills.add(new LoadedSkill(skill, from));
    }

    public boolean areSkillsLoaded(){
        return !this.loadedSkills.isEmpty();
    }

    public List<LoadedSkill> releaseLoadedSkills() {
        List<LoadedSkill> drained = new ArrayList<>(this.loadedSkills);
        this.loadedSkills.clear();
        return drained;
    }

    public void resetStaging() {
        this.slots.clearAll();
        this.loadedSkills.clear();
    }
}
