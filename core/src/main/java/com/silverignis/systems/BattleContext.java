package com.silverignis.systems;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.BattleVfx;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;

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

    /** Shared starburst texture for projectile-clash flourishes. Owned by {@code PlayState}. */
    public final Texture clashTexture;

    /** Post-set: CombatSystem constructor needs the context, so this is a cycle. */
    public CombatSystem combatSystem;

    private Vector2[][] tileCache;
    private float[]     depthCache;

    public BattleContext(Battlefield battlefield,
                         Player player,
                         List<Enemy> enemies,
                         List<BattleVfx> vfx,
                         GameEnvironment environment,
                         Texture clashTexture) {
        this.battlefield  = battlefield;
        this.player       = player;
        this.enemies      = enemies;
        this.vfx          = vfx;
        this.environment  = environment;
        this.clashTexture = clashTexture;
    }

    /** Call once after the viewport has been sized. Bakes all tile positions. */
    public void buildCache() {
        tileCache  = new Vector2[Battlefield.COLS][Battlefield.ROWS];
        depthCache = new float[Battlefield.ROWS];
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                tileCache[c][r] = environment.projectTile(c, r);
            }
        }
        for (int r = 0; r < Battlefield.ROWS; r++) {
            depthCache[r] = environment.tileDepthScale(r);
        }
    }

    public Vector2 projectedTileWorld(int col, int row) {
        if (tileCache != null) return tileCache[col][row];
        return environment.projectTile(col, row);
    }

    public float tileDepthScale(int row) {
        if (depthCache != null) return depthCache[row];
        return environment.tileDepthScale(row);
    }

    /** First alive enemy standing exactly on (col,row), or null. */
    public Enemy enemyAt(int col, int row) {
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            if (e.getCol() == col && e.getRow() == row) return e;
        }
        return null;
    }

    /** Alive enemies on a row. Fresh list; callers may iterate freely. */
    public List<Enemy> enemiesOnRow(int row) {
        List<Enemy> out = new ArrayList<>(enemies.size());
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            if (e.getRow() == row) out.add(e);
        }
        return out;
    }
}
