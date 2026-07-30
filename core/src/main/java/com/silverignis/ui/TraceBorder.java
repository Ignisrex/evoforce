package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Reward-card trace: two glowing pens start at top-center and draw the card border
 * down both sides symmetrically until they connect at bottom-center — then a flash
 * pulse on the border and a sheen band sweeping up the card face. Geometric quads
 * of the shared pixel texture (additive), so the line is uniform on all four edges —
 * particle sprites can't rotate to follow the path.
 * Sized/positioned via setBounds to the card's stage rect; removes itself when done.
 */
public final class TraceBorder extends Actor {

    private static final float THICKNESS = 0.045f;
    private static final float FLASH_TIME = 0.15f;
    private static final float SHEEN_TIME = 0.2f;
    private static final float FADE_TIME = 0.25f;

    private final Texture pixel;
    private final Color accent, bright;
    private final float traceTime;
    private float age;

    public TraceBorder(Texture pixel, Color accent, float traceTime, float delay) {
        this.pixel = pixel;
        this.accent = new Color(accent);
        this.bright = accent.cpy().lerp(Color.WHITE, 0.6f);
        this.traceTime = traceTime;
        this.age = -delay;
    }

    @Override
    public void act(float dt) {
        super.act(dt);
        age += dt;
        if (age >= traceTime + FADE_TIME) remove();
    }

    @Override
    public void draw(Batch b, float parentAlpha) {
        if (age <= 0f) return;
        float t = Math.min(age / traceTime, 1f);
        float post = age - traceTime;

        float glow = 1f;
        float alpha = 1f;
        if (post > 0f) {
            glow += 0.8f * Math.max(0f, 1f - post / FLASH_TIME);   // connect flash
            alpha = Math.max(0f, 1f - post / FADE_TIME);           // fade under the materialized card
        }
        if (alpha <= 0f) return;

        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float d = t * (getWidth() / 2f + getHeight() + getWidth() / 2f);   // per-pen path length

        b.setColor(bright.r, bright.g, bright.b, 0.9f * glow * alpha);
        drawPens(b, d);
        if (t < 1f) drawTips(b, d, glow);
        if (post > 0f && post < SHEEN_TIME) drawSheen(b, post / SHEEN_TIME);

        b.setColor(Color.WHITE);
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Both pens' covered path: along the top from center, down the side, along the bottom to center. */
    private void drawPens(Batch b, float d) {
        float w = getWidth(), h = getHeight();
        float left = getX(), right = left + w, bot = getY(), top = bot + h, cx = left + w / 2f;
        float halfW = w / 2f;

        float th = THICKNESS;
        float d1 = Math.min(d, halfW);
        b.draw(pixel, cx, top - th / 2f, d1, th);                       // right pen, top edge
        b.draw(pixel, cx - d1, top - th / 2f, d1, th);                  // left pen, top edge
        float d2 = Math.min(d - halfW, h);
        if (d2 > 0f) {
            b.draw(pixel, right - th / 2f, top - d2, th, d2);           // right edge, downward
            b.draw(pixel, left - th / 2f, top - d2, th, d2);            // left edge, downward
        }
        float d3 = Math.min(d - halfW - h, halfW);
        if (d3 > 0f) {
            b.draw(pixel, right - d3, bot - th / 2f, d3, th);           // bottom, right → center
            b.draw(pixel, left, bot - th / 2f, d3, th);                 // bottom, left → center
        }
    }

    private void drawTips(Batch b, float d, float glow) {
        float w = getWidth(), h = getHeight();
        float left = getX(), right = left + w, bot = getY(), top = bot + h, cx = left + w / 2f;
        float halfW = w / 2f;
        float ts = THICKNESS * 3.5f;

        float xR, yR;
        if (d < halfW)          { xR = cx + d;                 yR = top; }
        else if (d < halfW + h) { xR = right;                  yR = top - (d - halfW); }
        else                    { xR = right - (d - halfW - h); yR = bot; }
        b.setColor(1f, 1f, 1f, 0.9f * glow);
        b.draw(pixel, xR - ts / 2f, yR - ts / 2f, ts, ts);
        b.draw(pixel, cx - (xR - cx) - ts / 2f, yR - ts / 2f, ts, ts);   // left pen mirrors in x
    }

    /** Shiny-reveal band sweeping the card face bottom → top as the flash fades. */
    private void drawSheen(Batch b, float q) {
        float bandH = getHeight() * 0.35f;
        float yC = getY() + q * getHeight();
        float fade = 1f - q * q;
        strip(b, yC, bandH, 0.08f * fade);
        strip(b, yC, bandH / 3f, 0.12f * fade);
    }

    private void strip(Batch b, float yC, float bandH, float a) {
        float lo = Math.max(getY(), yC - bandH / 2f);
        float hi = Math.min(getY() + getHeight(), yC + bandH / 2f);
        if (hi <= lo) return;
        b.setColor(bright.r, bright.g, bright.b, a);
        b.draw(pixel, getX(), lo, getWidth(), hi - lo);
    }
}
