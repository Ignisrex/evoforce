package com.silverignis.systems;

import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.systems.combat.Combatant;

import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative battle roster plus the grid lookups every skill and the movement
 * system ask for. No rendering, no camera, no viewport — this is the half of the
 * old BattleContext that describes what is true about the fight.
 *
 * Still typed to Player/List&lt;Enemy&gt; rather than Combatant because the battle
 * scene reaches for the concrete types (shadow and HP-label views). Generalizing
 * that is the enemy-roster work, not this split.
 */
public final class BattleState {

    public final Player player;
    public final List<Enemy> enemies;

    public BattleState(Player player, List<Enemy> enemies) {
        this.player  = player;
        this.enemies = enemies;
    }

    public boolean tilesOccupied(int col, int row) {
        if (player.isAlive() && player.getCol() == col && player.getRow() == row) return true;
        for (Enemy e : enemies) {
            if (e.isAlive() && e.getCol() == col && e.getRow() == row) return true;
        }
        return false;
    }

    /** First alive *hittable* combatant on the tile (player checked first).
     *  Move-tween i-frames apply — a dodge input beats an incoming hit. */
    public Combatant combatantAt(int col, int row) {
        if (player.isAlive() && player.hittableAt(col, row)) return player;
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            if (e.hittableAt(col, row)) return e;
        }
        return null;
    }

    /** Alive, hittable combatants on the row whose team differs from the attacker (Beam pierce). */
    public List<Combatant> opposingOnRow(Combatant attacker, int row) {
        List<Combatant> out = new ArrayList<>();
        if (player.isAlive() && player.hittableOnRow(row) && player.getTeam() != attacker.getTeam()) {
            out.add(player);
        }
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            if (!e.hittableOnRow(row)) continue;
            if (e.getTeam() == attacker.getTeam()) continue;
            out.add(e);
        }
        return out;
    }
}
