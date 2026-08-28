package com.silverignis.skills.visuals;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.silverignis.animation.AnimController;
import com.silverignis.skills.elements.Element;

import java.util.Vector;

public final class VisualState {

    // ── anchors (updated every tick) ──
    /** Caster's feet; follows the combatant mid-dash. */
    public final Vector3 casterPos = new Vector3();
    /** The moving/placed thing: projectile center (incl. arc height), zone tile, beam origin. */
    public final Vector3 bodyPos = new Vector3();
    /** Valid only during/after an IMPACT or CLASH trigger. */
    public final Vector3 impactPos = new Vector3();

    // ── geometry (mostly set once at spawn) ──
    /** +1 = player-cast (faces east), -1 = enemy-cast. */
    public int dir;
    public int row;

    /** Columns raked by a strike this cast. */
    public int nearCol, farCol;
    /** Columns raked by a strike this cast. */
    public final IntArray hitTiles = new IntArray();

    // ── time (updated every tick) ──
    public Phase phase;
    /** 0→1 within the current phase. */
    public float phaseProgress;
    /** Seconds since CAST. */
    public float elapsed;

    public Element element;
    /** The one deliberate write-crossing: the visual decides whether the cast
     *  reads as a swing or an incantation. */
    public AnimController pose;
}
