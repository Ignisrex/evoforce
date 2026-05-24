package com.silverignis.registry;

import com.silverignis.components.Team;

public enum Monster {

    BEASTKIN("beastkin"),
    UNDEAD_BEASTKIN("undead_beastkin"),
    ECLIPSE_BEAST("eclipse_beast"),
    SKELETON("skeleton"),
    LICH("lich"),
    ELDER_LICH("elder_lich"),
    LICH_KING("lich_king"),
    LYCAN("lycan"),
    FENRIR("fenrir"),
    LIONEN("lionen"),
    NEMEAN("nemean");

    final String name;
    Monster(String name){
        this.name = name;
    }

    /**
     * Sprite file for the given side. Player-side combatants face east (the
     * {@code _se} frame); enemy-side face west ({@code _sw}). Matches the
     * ProjectileInstance team→direction convention (PLAYER → +1/east). This is
     * the single source of truth for the path: {@code GameAssets} loads with it,
     * {@code MonsterRegistry} fetches with it.
     */
    public String texturePath(Team team) {
        return "sprites/" + name + (team == Team.PLAYER ? "_se" : "_sw") + ".png";
    }
}
