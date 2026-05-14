package com.silverignis.entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

/**
 * Single-frame collision pass over the active set of projectiles plus the
 * two entities. Lean on purpose:
 *
 * <ul>
 *   <li>Pass A: opposing projectiles overlap &rarr; both are killed (cancel),
 *       and the world-space midpoint is appended to {@code clashPositions}
 *       so the caller can spawn a VFX there.</li>
 *   <li>Pass B: projectile vs opposing entity &rarr; projectile is killed.
 *       If it carries damage ({@code getDamage() > 0}) the entity takes that
 *       damage (which also flashes); otherwise it only flashes.</li>
 * </ul>
 *
 * <p>Order matters: cancels are resolved first so a projectile that meets
 * an opposing projectile on the same frame it would have hit an entity is
 * canceled instead of dealing the hit. The caller (e.g. {@code GameScreen})
 * is expected to cull dead projectiles after this runs.
 */
public final class CollisionResolver {

    private CollisionResolver() {}

    /**
     * @param clashPositions sink the resolver appends a {@link Vector2} to for
     *                       every cancel; caller owns the list and is
     *                       responsible for clearing it before each call.
     */
    public static void resolve(List<Projectile> projectiles,
                               Player player, Enemy enemy,
                               List<Vector2> clashPositions) {
        // Pass A: opposing projectiles cancel each other.
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile a = projectiles.get(i);
            if (!a.isAlive()) continue;
            for (int j = i + 1; j < projectiles.size(); j++) {
                Projectile b = projectiles.get(j);
                if (!b.isAlive() || a.getTeam() == b.getTeam()) continue;
                Rectangle ar = a.getBounds();
                // Snapshot a's center before b.getBounds() mutates the shared rect
                // (it doesn't here since they're different instances, but it's
                // cheap insurance).
                float ax = ar.x + ar.width * 0.5f;
                float ay = ar.y + ar.height * 0.5f;
                Rectangle br = b.getBounds();
                if (ar.overlaps(br)) {
                    float bx = br.x + br.width * 0.5f;
                    float by = br.y + br.height * 0.5f;
                    a.kill();
                    b.kill();
                    clashPositions.add(new Vector2((ax + bx) * 0.5f, (ay + by) * 0.5f));
                    break;
                }
            }
        }

        // Pass B: surviving projectiles hit opposing entities.
        for (Projectile p : projectiles) {
            if (!p.isAlive()) continue;
            if (p.getTeam() == Team.PLAYER) {
                if (enemy.isAlive() && p.getBounds().overlaps(enemy.getBounds())) {
                    if (p.getDamage() > 0) enemy.takeDamage(p.getDamage());
                    else                   enemy.flash();
                    p.kill();
                }
            } else {
                if (player.isAlive() && p.getBounds().overlaps(player.getBounds())) {
                    if (p.getDamage() > 0) player.takeDamage(p.getDamage());
                    else                   player.flash();
                    p.kill();
                }
            }
        }
    }
}
