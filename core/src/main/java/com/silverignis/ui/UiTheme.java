package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * Shared "Neural Link OS" palette + metrics — one set of numbers for the
 * staging overlay and the battle HUDs, so a tweak here moves everything
 * together.
 */
final class UiTheme {

    private UiTheme() {}

    // ── palette ─────────────────────────────────────────────────────────────
    static final Color CYAN      = rgb(0x00, 0xdb, 0xe7);
    static final Color CYAN_HI   = rgb(0x00, 0xf2, 0xff);
    static final Color GOLD      = rgb(0xff, 0xd8, 0x1d);
    static final Color TEXT      = rgb(0xdf, 0xe2, 0xeb);
    static final Color TEXT_DIM  = rgb(0xb9, 0xca, 0xcb);
    static final Color OUTLINE   = rgb(0x84, 0x94, 0x95);
    static final Color OUTLINE_V = rgb(0x3a, 0x49, 0x4b);
    static final Color PANEL     = rgba(0x18, 0x1c, 0x22, 0.86f);
    static final Color CARD      = rgba(0x26, 0x2a, 0x31, 0.90f);
    static final Color CARD_HI   = rgb(0x31, 0x35, 0x3c);
    static final Color SURF_LOW  = rgba(0x0a, 0x0e, 0x14, 0.85f);
    static final Color STATBOX   = rgba(0x1c, 0x20, 0x26, 0.9f);
    static final Color DIM       = rgba(0x00, 0x00, 0x00, 0.45f);

    static final Color CYAN_45      = withA(CYAN, 0.45f);
    static final Color OUTLINE_80   = withA(OUTLINE, 0.8f);
    static final Color OUTLINE_60   = withA(OUTLINE, 0.6f);
    static final Color OUTLINE_V_60 = withA(OUTLINE_V, 0.6f);
    static final Color CARD_70      = withA(CARD, 0.7f);

    // ── metrics (world units) ───────────────────────────────────────────────
    static final float BORDER        = 0.02f;
    static final float BORDER_THIN   = BORDER * 0.4f;  // dividers, stat boxes, charge bar
    static final float BORDER_HEAVY  = BORDER * 0.8f;  // corner brackets, cursor
    static final float CORNER_RADIUS = 0.07f;          // rounded corners on panels/cards/boxes
    static final float GLOW_WIDTH    = 0.10f;          // neon spill past panel/card borders

    static Color rgb(int r, int g, int b) { return new Color(r / 255f, g / 255f, b / 255f, 1f); }
    static Color rgba(int r, int g, int b, float a) { return new Color(r / 255f, g / 255f, b / 255f, a); }
    static Color withA(Color c, float a) { return new Color(c.r, c.g, c.b, a); }
}
