package com.silverignis.components;

/**
 * Identifies which side a combatant belongs to. Read by {@link Caster} (the
 * team the entity casts for) and by skill instances that need team-aware
 * direction or hit-target logic (notably {@code ProjectileInstance}).
 */
public enum Team { PLAYER, ENEMY }
