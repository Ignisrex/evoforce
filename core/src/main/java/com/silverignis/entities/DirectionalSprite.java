package com.silverignis.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.silverignis.components.Team;

/**
 * A billboard sprite with two horizontal facings, picked by battle side so the
 * two sides square off toward each other. Player-side combatants stand on the
 * west of the grid and face east (the {@code _se} frame); enemy-side combatants
 * stand on the east and face west (the {@code _sw} frame). This mirrors the
 * team→direction convention already used by {@code ProjectileInstance}
 * ({@code PLAYER → +1}, {@code ENEMY → -1}).
 */
public final class DirectionalSprite {

    /** south-east frame: faces east, shown for player-side combatants. */
    private final Sprite east;
    /** south-west frame: faces west, shown for enemy-side combatants. */
    private final Sprite west;

    public DirectionalSprite(Sprite east, Sprite west) {
        this.east = east;
        this.west = west;
    }

    /** The frame a combatant on the given team should draw. */
    public Sprite forTeam(Team team) {
        return team == Team.ENEMY ? west : east;
    }
}
