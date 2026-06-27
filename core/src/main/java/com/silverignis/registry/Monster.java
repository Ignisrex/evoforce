package com.silverignis.registry;

import com.silverignis.animation.AnimSheet;
import com.silverignis.components.Team;

import static com.silverignis.animation.AnimSheet.row;

import static com.silverignis.animation.AnimState.*;

public enum Monster {

    BEASTKIN       ("beastkin",        AnimSheet.of(row(IDLE, 7, 7f, true))),
    UNDEAD_BEASTKIN("undead_beastkin", AnimSheet.of(row(IDLE, 7, 7f, true))),
    ECLIPSE_BEAST  ("eclipse_beast",   AnimSheet.of(row(IDLE, 7, 7f, true))),
    SKELETON       ("skeleton",        AnimSheet.of(row(IDLE, 7, 7f, true))),
    LICH           ("lich",            AnimSheet.of(row(IDLE, 7, 7f, true))),
    ELDER_LICH     ("elder_lich",      AnimSheet.of(row(IDLE, 7, 7f, true))),
    LICH_KING      ("lich_king",       AnimSheet.of(row(IDLE, 7, 7f, true))),
    LYCAN          ("lycan",           AnimSheet.of(row(IDLE, 7, 7f, true))),
    FENRIR         ("fenrir",          AnimSheet.of(row(IDLE, 7, 7f, true))),
    LIONEN         ("lionen",          AnimSheet.of(row(IDLE, 8, 8f, true))),
    NEMEAN         ("nemean",          AnimSheet.of(row(IDLE, 7, 7f, true)));

    private final String name;
    private final AnimSheet animSheet;

    Monster(String name, AnimSheet animSheet) {
        this.name = name;
        this.animSheet = animSheet;
    }

    public AnimSheet animSheet()   { return animSheet; }

    /**
     * Per-state, per-direction sprite-sheet path. Each animation state ships its
     * own single-row strip in a {@code <state>/} subfolder, one file per facing
     * ({@code se.png} for {@link Team#PLAYER}, {@code sw.png} for {@link Team#ENEMY}).
     * The caller chooses the facing — a monster can appear on either team.
     */
    public String texturePath(com.silverignis.animation.AnimState state, Team facing) {
        String dir = facing == Team.PLAYER ? "se" : "sw";
        return "sprites/" + name + "/" + state.assetDir() + "/" + dir + ".png";
    }

    public static Monster fromName(String name) {
        for (Monster m : values()) {
            if (m.name.equals(name)) return m;
        }
        return null;
    }
}
