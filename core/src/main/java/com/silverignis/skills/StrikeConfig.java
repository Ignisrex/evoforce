package com.silverignis.skills;

/**
 * Shape config for {@link Skill.Shape#STRIKE}.
 *
 * <ul>
 *   <li>{@code dashTiles} — how many tiles the caster dashes forward before
 *       striking. {@code 1} matches the original {@code wind_strike} feel.</li>
 *   <li>{@code hitTiles} — how many tiles the strike connects with, starting
 *       one tile beyond the dash position. {@code 1} = single-tile poke (current
 *       wind_strike), {@code 2}+ = a wider sweep.</li>
 * </ul>
 */
public final class StrikeConfig implements ShapeConfig {

    private final int dashTiles;
    private final int hitTiles;

    public StrikeConfig(int dashTiles, int hitTiles) {
        this.dashTiles = dashTiles;
        this.hitTiles  = Math.max(1, hitTiles);
    }

    public int getDashTiles() { return dashTiles; }
    public int getHitTiles()  { return hitTiles; }
}
