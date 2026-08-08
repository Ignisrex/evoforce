package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

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
    final float delay;
    final boolean follow;
    final Val speed, life, size;
    final float spreadDeg;
    /** Rotation taking the +Y emission cone onto the aimed axis; null = default +Y (no rotation). */
    final Quaternion dirRot;
    final float driftX, driftY, driftZ;
    final float accelX, accelY, accelZ;
    final float jitterX, jitterY, jitterZ;
    final float offsetX, offsetY, offsetZ;
    final float sizeEndScale;
    final Interpolation sizeInterp;
    final float sizeBurstStart;
    final Val rotation, spin;
    final float fadeIn;
    final Interpolation alphaInterp;
    final float stretch;
    final float inherit;
    final float drag;
    final float wander;
    final Color colorFrom;
    final Color colorTo;
    final Interpolation colorInterp;
    /** Never null/empty — build() substitutes {defaultTexture} when unset.
     *  One entry = uniform effect; several = per-particle random pick, or an
     *  ordered over-life cycle when {@link #texturesOverLife} is set.
     *  Regions (wrapping the builder's Textures) so the draw can rotate. */
    final TextureRegion[] textures;
    final boolean texturesOverLife;
    final boolean additive;

    private EmitterSpec(Builder b) {
        this.mode = b.mode;
        this.rate = b.rate;
        this.count = b.count;
        this.window = b.window;
        this.delay = b.delay;
        this.follow = b.follow;
        this.speed = b.speed;
        this.life = b.life;
        this.size = b.size;
        this.spreadDeg = b.spreadDeg;
        this.dirRot = rotationFromY(b.dirX, b.dirY, b.dirZ);
        this.driftX = b.driftX;
        this.driftY = b.driftY;
        this.driftZ = b.driftZ;
        this.accelX = b.accelX;
        this.accelY = b.accelY;
        this.accelZ = b.accelZ;
        this.jitterX = b.jitterX;
        this.jitterY = b.jitterY;
        this.jitterZ = b.jitterZ;
        this.offsetX = b.offsetX;
        this.offsetY = b.offsetY;
        this.offsetZ = b.offsetZ;
        this.sizeEndScale = b.sizeEndScale;
        this.sizeInterp = b.sizeInterp;
        this.sizeBurstStart = b.sizeBurstStart;
        this.rotation = b.rotation;
        this.spin = b.spin;
        this.fadeIn = b.fadeIn;
        this.alphaInterp = b.alphaInterp;
        this.stretch = b.stretch;
        this.inherit = b.inherit;
        this.drag = b.drag;
        this.wander = b.wander;
        this.colorFrom = b.colorFrom;
        this.colorTo = b.colorTo;
        this.colorInterp = b.colorInterp;
        Texture[] src = (b.textures != null && b.textures.length > 0)
            ? b.textures : new Texture[]{ defaultTexture };
        this.textures = new TextureRegion[src.length];
        for (int i = 0; i < src.length; i++) this.textures[i] = new TextureRegion(src[i]);
        this.texturesOverLife = b.texturesOverLife;
        this.additive = b.additive;
    }

    private static Quaternion rotationFromY(float x, float y, float z) {
        if (x == 0f && z == 0f) {
            return y < 0f ? new Quaternion(Vector3.X, 180f) : null;   // straight down needs an explicit axis
        }
        return new Quaternion().setFromCross(Vector3.Y, new Vector3(x, y, z).nor());
    }

    //fluent, mutable; one instance per {@code .emitter(e -> ...)} lambda. Not thread-safe.
    public static final class Builder {
        private Mode mode = Mode.CONTINUOUS;
        private float rate = 30f;
        private int count;
        private float window;
        private float delay;
        private boolean follow;
        private Val speed = Val.of(2f), life = Val.of(0.8f), size = Val.of(0.25f);
        private float spreadDeg;
        private float dirX, dirY = 1f, dirZ;
        private float driftX, driftY, driftZ;
        private float accelX, accelY, accelZ;
        private float jitterX, jitterY, jitterZ;
        private float offsetX, offsetY, offsetZ;
        private float sizeEndScale = 1f;
        private Interpolation sizeInterp = Interpolation.linear;
        private float sizeBurstStart = 1f;
        private Val rotation = Val.of(0f), spin = Val.of(0f);
        private float fadeIn;
        private Interpolation alphaInterp = Interpolation.linear;
        private float stretch;
        private float inherit;
        private float drag;
        private float wander;
        private Color colorFrom = new Color(Color.WHITE), colorTo = new Color(Color.WHITE);
        private Interpolation colorInterp = Interpolation.linear;
        private Texture[] textures;
        private boolean texturesOverLife;
        private boolean additive = true;   // energy glow; mist/smoke want alphaBlend()

        public Builder continuous(float ratePerSec) { mode = Mode.CONTINUOUS; rate = ratePerSec; return this; }
        public Builder burst(int total) { return burst(total, 0f); }
        public Builder burst(int total, float window) { mode = Mode.BURST; count = total; this.window = window; return this; }

        /** Hold this emitter for the given seconds before it starts spawning — sequences
         *  layered effects (e.g. a second shockring) without life-time tricks. */
        public Builder delay(float seconds) { delay = seconds; return this; }

        /** Live particles ride the anchor: positions become offsets re-added to the anchor's
         *  current point every frame, so halos/glints stick to a moving caster or projectile
         *  instead of detaching. Don't pair with stateful anchors ({@code spiralIn}) — follow
         *  queries the anchor every render. */
        public Builder followAnchor() { follow = true; return this; }

        public Builder speed(Val v) { speed = v; return this; }
        public Builder speed(float c) { return speed(Val.of(c)); }
        public Builder life(Val v) { life = v; return this; }
        public Builder life(float c) { return life(Val.of(c)); }
        public Builder size(Val v) { size = v; return this; }
        public Builder size(float c) { return size(Val.of(c)); }
        public Builder spread(float halfAngleDeg) { spreadDeg = halfAngleDeg; return this; }

        /** Aim the emission cone along this axis instead of straight up (+Y). Applied before
         *  {@link #drift}, which stays a world-space wind regardless of aim. */
        public Builder direction(float x, float y, float z) { dirX = x; dirY = y; dirZ = z; return this; }

        /** Constant velocity bias added to every particle — a steady "wind" in world units. */
        public Builder drift(float x, float y, float z) { driftX = x; driftY = y; driftZ = z; return this; }

        /** Constant acceleration in world units/s² — e.g. negative Y for gravity arcs. */
        public Builder accel(float x, float y, float z) { accelX = x; accelY = y; accelZ = z; return this; }

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

        /** Scale spawn size by burst progress — first spawn × {@code startScale}, last × 1 —
         *  so a swept burst (e.g. a spiral anchor) grows along its path. Burst mode only. */
        public Builder sizeOverBurst(float startScale) { sizeBurstStart = startScale; return this; }

        /** Spawn angle in degrees (sampled per particle). */
        public Builder rotation(Val v) { rotation = v; return this; }
        public Builder rotation(float deg) { return rotation(Val.of(deg)); }

        /** Angular velocity in degrees/sec; a range spanning negatives spins both ways. */
        public Builder spin(Val v) { spin = v; return this; }
        public Builder spin(float degPerSec) { return spin(Val.of(degPerSec)); }

        /** Ramp alpha up over the first {@code fraction} of life instead of popping in at full. */
        public Builder fadeIn(float fraction) { fadeIn = fraction; return this; }

        /** Shape the fade-out curve (default linear — today's straight 1−t fade). */
        public Builder alphaOverLife(Interpolation interp) { alphaInterp = interp; return this; }

        /** Align the quad to its motion and stretch width by {@code 1 + k×speed} — sparks
         *  become streaks. Overrides rotation/spin: a streak's angle is its velocity. */
        public Builder stretch(float k) { stretch = k; return this; }

        /** Each spawn inherits the anchor's motion since the previous spawn as velocity,
         *  scaled by k — a moving anchor (e.g. {@code spiralIn}) sheds particles that fly
         *  along its path instead of hanging where they spawned. Pair with {@link #stretch}
         *  to turn them into motion-aligned streaks. */
        public Builder inherit(float k) { inherit = k; return this; }

        /** Velocity damping per second — burst fast, then hang (0 = none). */
        public Builder drag(float k) { drag = k; return this; }

        /** Random per-frame velocity churn (units/s²) — each particle meanders on its own,
         *  like dust disturbed by wind. Pair with {@link #drag} to keep the flutter bounded. */
        public Builder wander(float k) { wander = k; return this; }

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
