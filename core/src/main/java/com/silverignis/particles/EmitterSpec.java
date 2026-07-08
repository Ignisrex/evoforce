package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;

public class EmitterSpec {

    public enum Mode { CONTINUOUS, BURST }

    /** Fallback texture baked into every spec that doesn't set one. Wired once at
     *  startup ({@code Main.create()}); after that no downstream code (Emitter,
     *  ParticleEngine, Vfx) ever checks for a missing texture — {@link #textures}
     *  is guaranteed non-null and non-empty. */
    private static Texture defaultTexture;
    public static void init(Texture defaultTex) { defaultTexture = defaultTex; }

    final Mode mode;
    final float rate;
    final int count;
    final float window;
    final Val speed, life, size;
    final float spreadDeg;
    final float driftX, driftY, driftZ;
    final float jitterX, jitterY, jitterZ;
    final float offsetX, offsetY, offsetZ;
    final float sizeEndScale;
    final Interpolation sizeInterp;
    final Color colorFrom;
    final Color colorTo;
    final Interpolation colorInterp;
    /** Never null/empty — build() substitutes {defaultTexture} when unset.
     *  One entry = uniform effect; several = per-particle random pick, or an
     *  ordered over-life cycle when {@link #texturesOverLife} is set. */
    final Texture[] textures;
    final boolean texturesOverLife;
    final boolean additive;

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
        this.jitterX = b.jitterX;
        this.jitterY = b.jitterY;
        this.jitterZ = b.jitterZ;
        this.offsetX = b.offsetX;
        this.offsetY = b.offsetY;
        this.offsetZ = b.offsetZ;
        this.sizeEndScale = b.sizeEndScale;
        this.sizeInterp = b.sizeInterp;
        this.colorFrom = b.colorFrom;
        this.colorTo = b.colorTo;
        this.colorInterp = b.colorInterp;
        this.textures = (b.textures != null && b.textures.length > 0)
            ? b.textures : new Texture[]{ defaultTexture };
        this.texturesOverLife = b.texturesOverLife;
        this.additive = b.additive;
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
        private float jitterX, jitterY, jitterZ;
        private float offsetX, offsetY, offsetZ;
        private float sizeEndScale = 1f;
        private Interpolation sizeInterp = Interpolation.linear;
        private Color colorFrom = new Color(Color.WHITE), colorTo = new Color(Color.WHITE);
        private Interpolation colorInterp = Interpolation.linear;
        private Texture[] textures;
        private boolean texturesOverLife;
        private boolean additive = true;   // energy glow; mist/smoke want alphaBlend()

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

        /** Per-spawn random offset (± half-extents) added to the anchor point — spreads
         *  spawns across an area (e.g. a caster's body) instead of one point. */
        public Builder jitter(float hx, float hy, float hz) { jitterX = hx; jitterY = hy; jitterZ = hz; return this; }

        /** Constant shift off the anchor point — e.g. raise a body glow to mid-torso
         *  while a sibling ground-ring emitter stays at the feet. */
        public Builder offset(float x, float y, float z) { offsetX = x; offsetY = y; offsetZ = z; return this; }

        /** Mid-torso height above a feet-level anchor. */
        public static final float BODY_Y = 0.45f;

        /** Center this layer on the combatant's body instead of the feet-level anchor point. */
        public Builder atBody() { return offset(0f, BODY_Y, 0f); }

        /** Ramp size from the sampled initial to {@code endScale}× it over life (0 = shrink to nothing). */
        public Builder sizeOverLife(Interpolation interp, float endScale) {
            sizeInterp = interp; sizeEndScale = endScale; return this;
        }

        public Builder color(Color c) { colorFrom = new Color(c); colorTo = new Color(c); return this; }

        /** Lerp color from → to over life. Alpha caps peak opacity (then × the engine's 1−age/life fade). */
        public Builder colorOverLife(Interpolation interp, Color colorFrom, Color colorTo){
            colorInterp = interp;
            this.colorFrom = new Color(colorFrom);
            this.colorTo = new Color(colorTo);
            return this;
        }

        public Builder texture(Texture t) { return textures(t); }

        /** Texture set — each particle picks one at random when it spawns, so the
         *  effect isn't uniform (e.g. crackling lightning from varied spark sprites). */
        public Builder textures(Texture... ts) { textures = ts; texturesOverLife = false; return this; }

        /** Texture set played in the given order across each particle's life
         *  (frame = age/life), so a puff visibly evolves as it ages. */
        public Builder texturesOverLife(Texture... ts) { textures = ts; texturesOverLife = true; return this; }

        /** Normal alpha blending — soft translucent puffs (mist/smoke) instead of additive glow. */
        public Builder alphaBlend() { additive = false; return this; }

        EmitterSpec build() { return new EmitterSpec(this); }
    }


}
