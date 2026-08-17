package com.silverignis.systems.ai;

import com.badlogic.gdx.math.MathUtils;
import com.silverignis.components.Direction;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.systems.BattleState;
import com.silverignis.systems.MovementSystem;
import com.silverignis.systems.combat.Combatant;

import java.util.function.Predicate;

/**
 * Placeholder AI: a random step every 0.5-1.5s, and every 1.0-2.0s the first
 * off-cooldown deck skill whose shape could plausibly reach the player from the
 * current tile, falling back to the basic attack.
 *
 * Owns its own timers. Both halves used to live apart — the movement clock on
 * {@code Enemy}, the skill choice in {@code PlayState} — which is why an entity
 * had to be handed a MovementSystem to drive itself. Slated for overhaul; keep
 * the logic here so the rewrite can lift it cleanly.
 */
public final class EnemyAi {

    private static final float MIN_MOVE_INTERVAL   = 0.5f;
    private static final float MAX_MOVE_INTERVAL   = 1.5f;
    private static final float MIN_ATTACK_INTERVAL = 1.0f;
    private static final float MAX_ATTACK_INTERVAL = 2.0f;

    /** Enemies face west. */
    private static final int FACING = -1;

    private final Combatant combatant;

    private float moveTimer;
    private float moveInterval;
    private float attackTimer;
    private float attackInterval;

    public EnemyAi(Combatant combatant) {
        this.combatant = combatant;
        this.moveInterval   = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
        this.attackInterval = MathUtils.random(MIN_ATTACK_INTERVAL, MAX_ATTACK_INTERVAL);
    }

    public Combatant getCombatant() { return combatant; }

    /**
     * @param fire attempts the cast and reports whether it actually happened.
     *             The throttle only resets on a real cast, so a refused action
     *             is retried next frame instead of costing a turn.
     */
    public void update(float delta, BattleState battle, MovementSystem movement, Predicate<Skill> fire) {
        if (!combatant.isAlive()) return;

        // No status pre-check: tryGridStep owns the movement gate and refuses
        // on its own, so duplicating it here would only let the two disagree.
        moveTimer += delta;
        if (moveTimer >= moveInterval) {
            moveTimer = 0f;
            moveInterval = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
            movement.tryGridStep(combatant, Direction.values()[MathUtils.random(3)]);
        }

        attackTimer += delta;
        if (attackTimer < attackInterval) return;

        Skill chosen = pickAction(battle);
        if (chosen == null) return;
        if (!fire.test(chosen)) return;   // gate refused — keep the timer hot

        attackTimer = 0f;
        attackInterval = MathUtils.random(MIN_ATTACK_INTERVAL, MAX_ATTACK_INTERVAL);
    }

    /** Cooldown is checked here so a cooling skill is skipped in favour of a
     *  usable one, rather than chosen and then refused by the cast gate. */
    private Skill pickAction(BattleState battle) {
        var deck = combatant.getCaster().getDeck();
        for (Skill s : deck.all()) {
            if (deck.isOnCooldown(s)) continue;
            if (canReachPlayer(battle, s)) return s;
        }
        Skill basic = combatant.getCaster().getBasicAttack();
        if (basic != null && !deck.isOnCooldown(basic) && canReachPlayer(battle, basic)) {
            return basic;
        }
        return null;
    }

    private boolean canReachPlayer(BattleState battle, Skill skill) {
        int dr = battle.player.getRow() - combatant.getRow();
        int dc = battle.player.getCol() - combatant.getCol();

        switch (skill.getShape()) {
            case AURA:
                return true;
            case STRIKE:
                return dr == 0 && dc == 2 * FACING;
            case ZONE:
                return dr == 0 && dc == FACING;
            case BEAM:
                return dr == 0 && dc * FACING > 0;
            case PROJECTILE:
                if (dr != 0) return false;
                if (skill.getShapeConfig() instanceof ProjectileConfig) {
                    ProjectileConfig pc = (ProjectileConfig) skill.getShapeConfig();
                    if (pc.getMovementType() == ProjectileConfig.MovementType.LOB) {
                        return dc == pc.getTargetRange() * FACING;
                    }
                }
                return dc * FACING > 0;
            default:
                return false;
        }
    }
}
