package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

/** Filled, optionally-bordered rect — one SDF quad instead of five Images. */
final class Bezel extends Actor {
    private final RoundedRectShader shader;
    private Color fill, border, glow;
    private float borderWidth;
    private float radius;
    private float glowWidth;

    Bezel(RoundedRectShader shader) { this.shader = shader; }

    Bezel fill(Color c) { this.fill = c; return this; }
    Bezel border(Color c, float width) { this.border = c; this.borderWidth = width; return this; }
    Bezel radius(float r) { this.radius = r; return this; }
    Bezel glow(Color c, float width) { this.glow = c; this.glowWidth = width; return this; }

    @Override
    public void draw(Batch b, float parentAlpha) {
        b.setColor(1f, 1f, 1f, getColor().a * parentAlpha);
        shader.draw(b, getX(), getY(), getWidth(), getHeight(),
                    fill, border, borderWidth, radius, glow, glowWidth);
    }
}
