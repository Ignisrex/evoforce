package com.silverignis.systems.combat;

import com.badlogic.gdx.math.Vector3;
import com.silverignis.animation.AnimController;
import com.silverignis.components.*;
import com.silverignis.skills.Skill;

public interface Combatant {
    Health getHealth();
    Stats getStats();
    Caster getCaster();
    GridPosition getGridPosition();
    GridMovement getGridMovement();
    Team getTeam();
    StatusContainer getStatusContainer();
    AnimController getAnimController();

    int getCol();
    int getRow();
    default boolean hittableAt(int col, int row) {
        if (getGridMovement().stepProgress() < 0.5f) return false;
        return col == getCol() && row == getRow();
    }

    default boolean hittableOnRow(int row) {return hittableAt(getCol(), row); }

    // ── action gates ──────────────────────────────────────────────────────
    // Movement, attacking and casting are gated separately and on purpose: a
    // status may stop one without stopping the others (a root that still lets
    // you cast, a silence that still lets you swing). MovementSystem owns the
    // movement gate; these two are the other halves.

    /** Alive and not driven by a skill. Necessary for any action, sufficient for none. */
    default boolean canAct() { return isAlive() && !isInputLocked(); }

    /** Cooldown-free gates, so a caller can check before consuming a staged card. */
    default boolean canStartAttack() { return canAct() && !getStatusContainer().blocksAttack(); }
    default boolean canStartCast()   { return canAct() && !getStatusContainer().blocksCasting(); }

    /**
     * Whether {@code skill} may be started right now. Which status gate applies
     * depends on what the skill *is* to this combatant: its basic attack is an
     * attack, anything else is a cast.
     *
     * ponytail: basic-attack identity is the discriminator because it needs no
     * new data and is exactly right today. When a deck skill needs to count as
     * an attack, put the kind on Skill and switch on that instead.
     */
    default boolean canUse(Skill skill) {
        if (skill == null) return false;
        if (getCaster().getDeck().isOnCooldown(skill)) return false;
        return skill == getCaster().getBasicAttack() ? canStartAttack() : canStartCast();
    }

    /** Continuous grid position for drawing — fractional mid-step. Derived from
     *  the authoritative tile, so it cannot disagree with it. Screen coordinates
     *  are the render pass's business, not a combatant's. */
    default float getVisualCol() { return getGridMovement().visualCol(); }
    default float getVisualRow() { return getGridMovement().visualRow(); }
    // ponytail: tile-snapped, not smoothed — fine for feet emission; invert projection if you need sprite-glued anchors; for example particles on dashing sprite
    default void worldPos(Vector3 out) {getGridPosition().worldPos(out);}

    boolean isAlive();
    boolean isDead();
    boolean isInputLocked();
}
