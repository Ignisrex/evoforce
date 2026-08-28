package com.silverignis.systems;

import com.silverignis.components.Direction;
import com.silverignis.components.ManaPool;
import com.silverignis.components.ManaStats;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.systems.ai.EnemyAi;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.TriggerBus;
import com.silverignis.traits.TraitsContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * One battle: its roster, its systems, and its per-frame tick. Knows nothing
 * about screens, cameras or the run — it is handed a player and a roster, and
 * reports an {@link Outcome} rather than acting on one.
 *
 * Unlike the Godot port, this exposes a single {@link #tick} instead of separate
 * phases. It can: libGDX runs {@code input()} before {@code update()}, so the
 * loaded-burst release already happens outside the tick. In Godot everything
 * lives in {@code _Process}, which forces the release between two phases and so
 * forces the phases apart.
 */
public final class Encounter {

    public enum Outcome { ONGOING, VICTORY, DEFEAT }

    private final BattleState battleState;
    private final MovementSystem movementSystem;
    private final CombatSystem combatSystem;
    private final List<EnemyAi> enemyAis = new ArrayList<>();
    private final ManaPool mana;

    // Not exposed: nothing outside the encounter has any business publishing
    // trigger events or applying damage directly.
    private final TriggerBus triggerBus;
    private final DamageSystem damageSystem;

    /**
     * Step order matters: the combatants and their systems exist first, and only
     * then do traits apply — slot capacity is recomputed absolute, and every
     * combatant subscribes to the fresh per-battle bus.
     */
    private Encounter(Player player, List<Enemy> enemies, ManaStats manaStats, ParticleEngine particles) {
        this.mana           = new ManaPool(manaStats);
        this.triggerBus     = new TriggerBus();
        this.damageSystem   = new DamageSystem(triggerBus);
        this.battleState    = new BattleState(player, enemies);
        this.movementSystem = new MovementSystem(battleState);
        this.combatSystem   = new CombatSystem(battleState, damageSystem, triggerBus,
                                               movementSystem, particles);

        for (Enemy e : enemies) enemyAis.add(new EnemyAi(e));

        TraitsContainer traits = player.getCaster().getTraits();
        player.getSlots().setSlotCapacity(SkillSlots.BASE_CAPACITY + traits.slotCapacityBonus());
        traits.applyBattleHooks(player, triggerBus, damageSystem);
        for (Enemy e : enemies) e.getCaster().getTraits().applyBattleHooks(e, triggerBus, damageSystem);
    }

    /**
     * Assemble a battle from an already-built player and roster. The mana bar is
     * built here and dies with the fight; only its capacity and regen come from
     * the run, so no caller has to remember to reset it.
     */
    public static Encounter create(Player player, List<Enemy> enemies,
                                   ManaStats manaStats, ParticleEngine particles) {
        return new Encounter(player, enemies, manaStats, particles);
    }

    // ── accessors ─────────────────────────────────────────────────────────

    public BattleState battleState() { return battleState; }
    public Player player()           { return battleState.player; }
    public List<Enemy> enemies()     { return battleState.enemies; }
    public ManaPool mana()           { return mana; }

    /** Public because presentation submits its renderables and drains its tiles.
     *  Wrapping the whole API in delegates would be ceremony. */
    public CombatSystem combat() { return combatSystem; }

    // ── per-frame ─────────────────────────────────────────────────────────

    /** Order is the legacy PlayState ordering and is load-bearing. */
    public void tick(float delta) {
        battleState.player.update(delta);
        for (Enemy e : battleState.enemies) e.update(delta);

        mana.update(delta);

        for (EnemyAi ai : enemyAis) {
            ai.update(delta, battleState, movementSystem,
                      skill -> combatSystem.tryCast(ai.getCombatant(), skill));
        }

        combatSystem.tickStatuses(delta);
        combatSystem.update(delta);
    }

    /**
     * Victory once every enemy is dead *and* finished fading — {@code isDead()}
     * waits on the death animation, which is what the transition delay rides on.
     * An empty roster counts as cleared.
     */
    public Outcome outcome() {
        if (!battleState.player.isAlive()) return Outcome.DEFEAT;
        for (Enemy e : battleState.enemies) if (!e.isDead()) return Outcome.ONGOING;
        return Outcome.VICTORY;
    }

    // ── actions ───────────────────────────────────────────────────────────

    public boolean tryMove(Combatant combatant, Direction dir) {
        return movementSystem.tryGridStep(combatant, dir);
    }

    public boolean tryCast(Combatant combatant, Skill skill) {
        return combatSystem.tryCast(combatant, skill);
    }

    public void cast(Combatant combatant, Skill skill) {
        combatSystem.cast(combatant, skill);
    }

    public void loadSkill(Combatant combatant, Skill skill) {
        combatSystem.loadSkill(combatant, skill);
    }

    public void releaseLoadedSkills(Combatant combatant) {
        combatSystem.resolveLoadedSkills(combatant);
    }

    public void collectCoveredTiles(SkillInstance.TileSink sink) {
        combatSystem.collectCoveredTiles(sink);
    }

    /** Battle-end cleanup: cancel every running skill so its visuals free themselves. */
    public void finish() {
        combatSystem.finishAll();
    }

    /**
     * Slots and any loaded burst are per-battle — they must not ride the
     * persistent profile into the next fight.
     *
     * Does not drain mana, matching existing behaviour: the pool is drained when
     * a GameScreen is built, not when one ends. The Godot port drains here
     * instead. Part of the unresolved question of who owns the pool's lifetime.
     */
    public void resetStaging() {
        battleState.player.getCaster().resetStaging();
    }
}
