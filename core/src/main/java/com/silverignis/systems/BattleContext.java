package com.silverignis.systems;

import com.badlogic.gdx.math.Vector2;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.BattleVfx;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.environment.GameEnvironment;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.TriggerBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Pass-around bag of references a skill or system needs during a battle.
 * Constructed once by {@code PlayState}.
 */
public class BattleContext {

    public final Battlefield battlefield;
    public final Player player;
    public final List<Enemy> enemies;
    public final GameEnvironment environment;

    /**
     * Sink for short-lived visual effects. {@code PlayState} owns the list
     * and culls finished entries each frame.
     */
    public final List<BattleVfx> vfx;

    public final DamageSystem damageSystem;
    public final TriggerBus triggerBus;

    /** Post-set: CombatSystem constructor needs the context, so this is a cycle. */
    public CombatSystem combatSystem;
    public ParticleEngine particleEngine;

    public final MovementSystem movementSystem;

    private Vector2[][] tileCache;
    private float[]     depthCache;

    public BattleContext(Battlefield battlefield,
                         Player player,
                         List<Enemy> enemies,
                         List<BattleVfx> vfx,
                         GameEnvironment environment,
                         DamageSystem damageSystem,
                         TriggerBus triggerBus,
                         MovementSystem movementSystem) {
        this.battlefield  = battlefield;
        this.player       = player;
        this.enemies      = enemies;
        this.vfx          = vfx;
        this.environment  = environment;
        this.damageSystem = damageSystem;
        this.triggerBus = triggerBus;
        this.movementSystem = movementSystem;
        this.movementSystem.setBattleContext(this);
    }

    /** Call once after the viewport has been sized. Bakes all tile positions. */
    public void buildCache() {
        tileCache  = new Vector2[Battlefield.COLS][Battlefield.ROWS];
        depthCache = new float[Battlefield.ROWS];
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                tileCache[c][r] = environment.project(battlefield.floorX(c), battlefield.floorZ(r));
            }
        }
        for (int r = 0; r < Battlefield.ROWS; r++) {
            depthCache[r] = environment.depthScale(battlefield.floorZ(r));
        }
    }

    public Vector2 projectedTileWorld(int col, int row) {
        if (tileCache != null) return tileCache[col][row];
        return environment.project(battlefield.floorX(col), battlefield.floorZ(row));
    }

    public float tileDepthScale(int row) {
        if (depthCache != null) return depthCache[row];
        return environment.depthScale(battlefield.floorZ(row));
    }

    public boolean tilesOccupied(int col, int row) {
        if (player.isAlive() && player.getCol() == col && player.getRow() == row) return true;
        for (Enemy e: enemies){
            if (e.isAlive() && e.getCol() == col && e.getRow() == row) return  true;
        }
        return false;
    }

    public Combatant combatantAt(int col, int  row){
        if(player.isAlive() && player.hittableAt(col, row) ) return player;
        for(Enemy e:  enemies){
            if (!e.isAlive()) continue;
            if (e.hittableAt(col, row)) return e;
        }
        return null;
    }

    public List<Combatant> opposingOnRow(Combatant attacker, int row){
        List<Combatant> out = new ArrayList<>();
        if (player.isAlive() && player.hittableOnRow(row) && player.getTeam() != attacker.getTeam()){
            out.add(player);
        }

        for (Enemy e : enemies){
            if(!e.isAlive()) continue;
            if(!e.hittableOnRow(row)) continue;
            if(e.getTeam() == attacker.getTeam()) continue;
            out.add(e);
        }

        return out;
    }
}
