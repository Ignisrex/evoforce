package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;

public class EmitterSpec {

    public enum Mode { CONTINUOUS, BURST }

    final Mode mode;
    final float rate;
    final int count;
    final float window;
    final Val speed, life, size;
    final float spreadDeg;
    final float driftX, driftY, driftZ;
    final float sizeEndScale;
    final Interpolation sizeInterp;
    final Color colorFrom;
    final Color colorTo;
    final Interpolation colorInterp;
    final Texture texture;

    private EmitterSpec(Builder b) {
        this.mode = b.mode;
        this.rate = b.rate;
        this.count = b.count;
        this.window = b.window;
        this.speed = b.speed;
        this.life = b.life;
        this.size = b.size;
        this.spreadDeg = b.spreadDeg;
        this.driftX = b.driftX;
        this.driftY = b.driftY;
        this.driftZ = b.driftZ;
        this.sizeEndScale = b.sizeEndScale;
        this.sizeInterp = b.sizeInterp;
        this.colorFrom = b.colorFrom;
        this.colorTo = b.colorTo;
        this.colorInterp = b.colorInterp;
        this.texture = b.texture;
    }

    //fluent, mutable; one instance per {@code .emitter(e -> ...)} lambda. Not thread-safe.
    public static final class Builder {
        private Mode mode = Mode.CONTINUOUS;
        private float rate = 30f;
        private int count;
        private float window;
        private Val speed = Val.of(2f), life = Val.of(0.8f), size = Val.of(0.25f);
        private float spreadDeg;
        private float driftX, driftY, driftZ;
        private float sizeEndScale = 1f;
        private Interpolation sizeInterp = Interpolation.linear;
        private Color colorFrom = new Color(Color.WHITE), colorTo = new Color(Color.WHITE);
        private Interpolation colorInterp = Interpolation.linear;
        private Texture texture;

        public Builder continuous(float ratePerSec) { mode = Mode.CONTINUOUS; rate = ratePerSec; return this; }
        public Builder burst(int total) { return burst(total, 0f); }
        public Builder burst(int total, float window) { mode = Mode.BURST; count = total; this.window = window; return this; }

        public Builder speed(Val v) { speed = v; return this; }
        public Builder speed(float c) { return speed(Val.of(c)); }
        public Builder life(Val v) { life = v; return this; }
        public Builder life(float c) { return life(Val.of(c)); }
        public Builder size(Val v) { size = v; return this; }
        public Builder size(float c) { return size(Val.of(c)); }
        public Builder spread(float halfAngleDeg) { spreadDeg = halfAngleDeg; return this; }
        /** Constant velocity bias added to every particle — a steady "wind" in world units. */
        public Builder drift(float x, float y, float z) { driftX = x; driftY = y; driftZ = z; return this; }

        /** Ramp size from the sampled initial to {@code endScale}× it over life (0 = shrink to nothing). */
        public Builder sizeOverLife(Interpolation interp, float endScale) {
            sizeInterp = interp; sizeEndScale = endScale; return this;
        }

        public Builder color(Color c) { colorFrom = new Color(c); colorTo = new Color(c); return this; }

        /** Lerp RGB from → to over life (alpha handled by the engine's age fade, so use opaque colors). */
        public Builder colorOverLife(Interpolation interp, Color colorFrom, Color colorTo){
            colorInterp = interp;
            this.colorFrom = new Color(colorFrom);
            this.colorTo = new Color(colorTo);
            return this;
        }

        public Builder texture(Texture t) { texture = t; return this; }

        EmitterSpec build() { return new EmitterSpec(this); }
    }


}
