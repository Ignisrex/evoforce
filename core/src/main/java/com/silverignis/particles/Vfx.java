package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.silverignis.assets.GameAssets;
import com.silverignis.entities.Battlefield;
import com.silverignis.skills.elements.Element;

import static com.silverignis.particles.Val.of;
import static com.silverignis.particles.Val.range;

public final class Vfx {

    private Vfx() {}

    private static GameAssets assets;
    public static void init(GameAssets a) { assets = a; }

    public static EffectDef ambientEmbers() {
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(60f)
                .speed(range(2f, 3f)).life(of(0.8f)).size(of(0.25f)).spread(25f)
                .color(new Color(1f, 0.6f, 0.2f, 1f)))
            .build();
    }

    public static EffectDef spark(Element element) {
        Color tint = tint(element);
        return EffectDef.effect()
            .emitter(e -> e
                .burst(24, 0.1f)
                .speed(range(2f, 5f)).life(range(0.4f,0.8f)).size(range(0.2f, 0.35f)).spread(180f)
                .stretch(0.25f).drag(2.5f)   // streak out fast, then hang and die
                .sizeOverLife(Interpolation.pow2In, 0f)
                .texture(assets.star(4))
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    /** Crackling electric discharge — each shard is a random spark_* sprite so the
     *  lightning coming off isn't uniform. Continuous while driven (beam FIRE→FADE). */
    public static EffectDef crackle(Element element) {
        Color tint = tint(element);
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(120f)
                .speed(range(1.5f, 4f)).life(range(0.1f, 0.35f)).size(range(0.15f, 0.45f)).spread(180f)
                .textures(
                    assets.spark(1), assets.spark(2), assets.spark(3), assets.spark(4),
                    assets.spark(5), assets.spark(6), assets.spark(7))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    /** Layered projectile-impact burst — all emitters fire simultaneously on one anchor
     *  (the point where the projectiles meet). Read: flash → ring/debris →
     *  tumbling sparkles/smoke. */
    public static EffectDef impact(Element element) {
        Color tint  = tint(element);
        Color smoke = new Color(0.55f, 0.55f, 0.6f, 0.35f);   // cool grey, translucent
        return EffectDef.effect()
            // 1. Shockwave ring
            /*.emitter(e -> e
                .burst(1).speed(0f).life(0.2f).size(0.4f)
                .texture(assets.circle(1))
                .sizeOverLife(Interpolation.pow2Out, 1.25f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            // 2. Flare streak — lens-glint over the core, a touch longer.
            /*.emitter(e -> e
                .burst(1).speed(0f).life(0.18f).size(0.8f)
                .texture(assets.flare(1))
                .sizeOverLife(Interpolation.pow2Out, 1.5f)
                .color(Color.WHITE))*/
            // 3. Main Flash
            /*.emitter(e -> e
                .burst(1).speed(0f).life(0.3f).size(0.7f)
                .textures(assets.star(8), assets.starA(8))
                .sizeOverLife(Interpolation.pow2Out, 2.4f)
                .color(tint))*/
            // 4. Mana dust — the explosion displaces it radially out of the clash point:
            // every mote flies outward from the center (full-sphere spread, near-zero
            // jitter), drag bleeding the blast off while wander keeps each one fluttering.
            .emitter(e -> e
                .burst(10, 0.08f)
                .speed(range(0.6f, 1.5f)).life(range(0.5f, 0.9f)).size(range(0.1f, 0.22f)).spread(180f)
                .jitter(0.03f, 0.03f, 0.03f)
                .wander(35f).drag(1.2f)
                .stretch(0.5f)
                .alphaOverLife(Interpolation.pow2In)
                .textures(
                    assets.spotlightA(1), assets.spotlightA(2), assets.spotlightA(3), assets.spotlightA(4),
                    assets.spotlightA(5), assets.spotlightA(6), assets.spotlightA(7), assets.spotlightA(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            // 6. Smoke — soft lingering body, the slow tail of the read.
            .emitter(e -> e
                .burst(3, 0.1f)
                .speed(range(0.15f, 0.4f)).life(range(0.7f, 1.2f)).size(range(0.35f, 0.55f)).spread(180f)
                .drift(0f, 0.2f, 0f)
                .spin(range(-40f, 40f)).fadeIn(0.2f)
                .alphaBlend()
                .texturesOverLife(
                    assets.smoke(1), assets.smoke(3), assets.smoke(5), assets.smoke(7), assets.smoke(9))
                .sizeOverLife(Interpolation.pow2Out, 1.6f)
                .colorOverLife(Interpolation.linear, smoke, new Color(smoke.r, smoke.g, smoke.b, 0f)))
            .build();
    }

    /** Fire trail riding a projectile's travel anchor — the sprite stays the core; this is
     *  everything coming off it. Read: glow + flare glint on the ball → flames licking
     *  behind → embers → soft smoke tail. */
    public static EffectDef fireTrail(int dir) {
        Color orange = new Color(1f, 0.6f, 0.25f, 1f);
        Color red    = new Color(1f, 0.3f, 0.15f, 1f);
        Color smoke  = new Color(0.5f, 0.48f, 0.5f, 0.3f);
        return EffectDef.effect()
            // 1. Core glow — hugs the ball; short life so it doesn't smear (regen-halo trick).
            .emitter(e -> e
                .continuous(12f)
                .speed(0f).life(range(0.15f, 0.25f)).size(range(0.45f, 0.6f))
                .jitter(0.05f, 0.05f, 0f)
                .texture(assets.light(1))
                .color(new Color(1f, 0.65f, 0.3f, 0.7f)))
            // 2. Flare glint — a bigger streak pulsing over the core.
            .emitter(e -> e
                .continuous(5f)
                .speed(0f).life(range(0.15f, 0.25f)).size(range(0.75f, 0.95f))
                .texture(assets.flare(1))
                .color(new Color(1f, 0.7f, 0.35f, 0.8f)))
            // 3. Flame tongues — lick backward off the ball.
            .emitter(e -> e
                .continuous(18f)
                .speed(range(0.05f, 0.2f)).life(range(0.25f, 0.45f)).size(range(0.2f, 0.35f)).spread(30f)
                .jitter(0.12f, 0.12f, 0f)
                .drift(-dir * 1.2f, 0.2f, 0f)
                .textures(
                    assets.flame(1), assets.flame(2), assets.flame(3),
                    assets.flame(4), assets.flame(5), assets.flame(6))
                .sizeOverLife(Interpolation.linear, 1.25f)
                .colorOverLife(Interpolation.linear, orange, red))
            // 4. Ember sparks — falling back and up, snuffing out.
            .emitter(e -> e
                .continuous(12f)
                .speed(range(0.1f, 0.3f)).life(range(0.3f, 0.5f)).size(range(0.06f, 0.14f)).spread(60f)
                .jitter(0.1f, 0.1f, 0f)
                .drift(-dir * 1.0f, 0.35f, 0f)
                .texture(assets.star(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, orange))
            // 5. Smoke wisps — the soft tail end.
            .emitter(e -> e
                .continuous(6f)
                .speed(range(0.05f, 0.15f)).life(range(0.5f, 0.9f)).size(range(0.25f, 0.4f)).spread(40f)
                .jitter(0.1f, 0.1f, 0f)
                .drift(-dir * 0.8f, 0.3f, 0f)
                .alphaBlend()
                .texturesOverLife(
                    assets.smoke(2), assets.smoke(4), assets.smoke(6), assets.smoke(8))
                .sizeOverLife(Interpolation.pow2Out, 1.6f)
                .colorOverLife(Interpolation.linear, smoke, new Color(smoke.r, smoke.g, smoke.b, 0f)))
            .build();
    }

    /** Void trail riding a projectile's travel anchor — fireTrail's dark twin. The drama
     *  lives ON the ball (flickering violet arcs + a tight halo, both followAnchor so they
     *  ride it) and in chaotic motes leaking in every direction. */
    public static EffectDef darkTrail(int dir) {
        Color violet = new Color(0.75f, 0.45f, 1f, 1f);
        Color deep   = new Color(0.35f, 0.1f, 0.6f, 1f);
        Color smoke  = new Color(0.28f, 0.16f, 0.4f, 0.35f);
        return EffectDef.effect()
            // 1. Core halo — rides the ball; short life so nothing lingers at impact.
            .emitter(e -> e
                .continuous(20f)
                .followAnchor()
                .speed(0f).life(of(0.1f)).size(range(0.4f, 0.55f))
                .jitter(0.04f, 0.04f, 0f)
                .texture(assets.light(1))
                .color(new Color(0.6f, 0.3f, 1f, 0.75f)))
            // 2. Dark lightning — violet arcs flickering over the ball, each shard morphing
            // through spark frames. Brief by design: lightning flickers.
            .emitter(e -> e
                .continuous(10f)
                .followAnchor()
                .speed(0f).life(range(0.12f, 0.2f)).size(range(0.3f, 0.5f))
                .jitter(0.15f, 0.15f, 0f)
                .texturesOverLife(assets.spark(1), assets.spark(3), assets.spark(5), assets.spark(7))
                .colorOverLife(Interpolation.linear, violet, deep))
            // 3. Unstable motes — energy leaking off in ALL directions, not a neat tail.
            .emitter(e -> e
                .continuous(28f)
                .speed(range(0.15f, 0.45f)).life(range(0.3f, 0.55f)).size(range(0.1f, 0.2f)).spread(180f)
                .jitter(0.15f, 0.15f, 0f)
                .drift(-dir * 1.6f, 0.05f, 0f)
                .textures(assets.magic(3), assets.magic(4), assets.star(9))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, violet, deep))
            // 4. Void smoke wisps — the soft tail end.
            .emitter(e -> e
                .continuous(12f)
                .speed(range(0.05f, 0.15f)).life(range(0.5f, 0.9f)).size(range(0.25f, 0.4f)).spread(40f)
                .jitter(0.1f, 0.1f, 0f)
                .drift(-dir * 1.6f, 0.15f, 0f)
                .alphaBlend()
                .texturesOverLife(
                    assets.smoke(2), assets.smoke(4), assets.smoke(6), assets.smoke(8))
                .sizeOverLife(Interpolation.pow2Out, 1.5f)
                .colorOverLife(Interpolation.linear, smoke, new Color(smoke.r, smoke.g, smoke.b, 0f)))
            .build();
    }

    /** Electric field: lightning striking outward FROM the anchor — bolts are pinned in
     *  place (no velocity) and point out in every direction, each one morphing through
     *  the spark frames over its life; a separate layer of free-moving radical motes
     *  supplies the motion so the bolts themselves never fly off. Works on the tile
     *  (electro zone) and on the ball in flight. */
    public static EffectDef electricArcs(Element element) {
        Color tint = tint(element);
        return EffectDef.effect()
            // 1. Striking bolts — anchored around the tile, flickering through spark frames.
            .emitter(e -> e
                .continuous(10f)
                .speed(0f).life(range(0.15f, 0.25f)).size(range(0.35f, 0.6f))
                .jitter(0.35f, 0.15f, 0.25f)
                .texturesOverLife(
                    assets.spark(1), assets.spark(2), assets.spark(3), assets.spark(4),
                    assets.spark(5), assets.spark(6), assets.spark(7))
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            // 2. Free radicals — small charged motes zipping off chaotically.
            .emitter(e -> e
                .continuous(12f)
                .speed(range(0.3f, 0.8f)).life(range(0.3f, 0.6f)).size(range(0.08f, 0.16f)).spread(180f)
                .jitter(0.2f, 0.1f, 0.15f)
                .textures(assets.star(4), assets.star(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            // 3. Center glow — brief pulse so the field has a bright heart; short life, no smear.
            .emitter(e -> e
                .continuous(8f)
                .speed(0f).life(of(0.15f)).size(range(0.45f, 0.6f))
                .texture(assets.light(1))
                .color(new Color(tint.r, tint.g, tint.b, 0.5f)))
            .build();
    }

    /** Toxic vapor rolling off a poisoned tile — soft sickly-green puffs that
     *  bloom as they rise and thin out, with a few bubbles popping up from the
     *  ooze underneath. Continuous while the zone is active. */
    public static EffectDef toxicClouds() {
        Color thick = new Color(0.45f, 0.85f, 0.25f, 0.45f);   // dense near the ground
        Color thin  = new Color(0.55f, 0.90f, 0.35f, 0f);      // faded out as it rises
        return EffectDef.effect()
            // 1. Vapor puffs — slow, rolling, translucent.
            .emitter(e -> e
                .continuous(9f)
                .speed(range(0.05f, 0.15f)).life(range(1.0f, 1.8f)).size(range(0.35f, 0.6f)).spread(50f)
                .jitter(0.4f, 0.02f, 0.25f)
                .drift(0f, 0.35f, 0f)
                .spin(range(-20f, 20f)).fadeIn(0.3f)
                .alphaBlend()
                .texturesOverLife(assets.smoke(1), assets.smoke(3), assets.smoke(5), assets.smoke(7), assets.smoke(9))
                .sizeOverLife(Interpolation.pow2Out, 1.8f)
                .colorOverLife(Interpolation.linear, thick, thin))
            // 2. Bubbles — small bright dots popping up and vanishing fast.
            .emitter(e -> e
                .continuous(5f)
                .speed(range(0.2f, 0.4f)).life(range(0.25f, 0.45f)).size(range(0.06f, 0.12f)).spread(20f)
                .jitter(0.35f, 0f, 0.2f)
                .drift(0f, 0.5f, 0f)
                .texture(assets.circle(1))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .color(new Color(0.7f, 1f, 0.5f, 0.9f)))
            .build();
    }

    /** Small glowing motes rising off an energized surface. */
    public static EffectDef energyMotes(Element element) {
        Color tint = tint(element);
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(14f)
                .speed(range(0.1f, 0.3f)).life(range(0.4f, 0.8f)).size(range(0.06f, 0.14f)).spread(180f)
                .jitter(0.3f, 0.05f, 0.2f)
                .drift(0f, 0.8f, 0f)
                .textures(assets.star(4), assets.star(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    public static EffectDef beamEmbers(Color tint, int dir) {
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(300f)
                .speed(range(1f, 2.5f)).life(range(0.5f, 0.1f)).size(range(0.1f, 0.4f)).spread(180f)
                .drift(-dir * 1.5f, 0f, 0f)   // slight lean back toward the beam's origin (caster side)
                .texture(assets.star(8))   // ember glow
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    /** Red flames licking off a beam + sooty smoke — fire's answer to beamEmbers */
    public static EffectDef beamFlames(int dir) {
        Color red   = new Color(1f, 0.35f, 0.15f, 1f);
        Color deep  = new Color(0.7f, 0.1f, 0.05f, 1f);
        Color smoke = new Color(0.35f, 0.3f, 0.3f, 0.35f);
        return EffectDef.effect()
            // 1. Flame tongues — licking up and back off the beam
            .emitter(e -> e
                .continuous(150f)
                .speed(range(0.7f, 1.3f)).life(range(0.25f, 0.5f)).size(range(0.2f, 0.45f)).spread(60f)
                .direction(-dir * 0.8f, 0.6f, 0f)
                .textures(
                    assets.flame(1), assets.flame(2), assets.flame(3),
                    assets.flame(4), assets.flame(5), assets.flame(6))
                .sizeOverLife(Interpolation.linear, 1.3f)
                .colorOverLife(Interpolation.linear, red, deep))
            // 2. Smoke wisps
            .emitter(e -> e
                .continuous(30f)
                .speed(range(0.1f, 0.3f)).life(range(0.6f, 1.1f)).size(range(0.3f, 0.55f)).spread(50f)
                .drift(-dir * 0.5f, 0.5f, 0f)
                .spin(range(-30f, 30f)).fadeIn(0.25f)
                .alphaBlend()
                .texturesOverLife(
                    assets.smoke(2), assets.smoke(4), assets.smoke(6), assets.smoke(8))
                .sizeOverLife(Interpolation.pow2Out, 1.8f)
                .colorOverLife(Interpolation.linear, smoke, new Color(smoke.r, smoke.g, smoke.b, 0f)))
            .build();
    }

    /** Cold fog creeping off a frozen tile — low, slow, translucent, with the odd glint of frost. */
    public static EffectDef frostFog() {
        Color thick = new Color(0.80f, 0.90f, 1.00f, 0.40f);   // dense near the ice
        Color thin  = new Color(0.60f, 0.80f, 1.00f, 0f);      // gone as it rises
        return EffectDef.effect()
            // 1. Fog — cold air is heavy: it pools over the slab, spreads out and sinks,
            //    never rises. Spawned across the panel (1.25 x 1.0) just above the floor,
            //    near-zero speed so it stays on the tile and only blooms in place.
            .emitter(e -> e
                .continuous(8f)
                .speed(range(0f, 0.04f)).life(range(1.0f, 1.6f)).size(range(0.35f, 0.55f)).spread(180f)
                .jitter(0.45f, 0.02f, 0.3f)
                .offset(0f, 0.12f, 0f)
                .drift(0f, -0.06f, 0f)
                .spin(range(-12f, 12f)).fadeIn(0.4f)
                .alphaBlend()
                .texturesOverLife(assets.smoke(2), assets.smoke(4), assets.smoke(6), assets.smoke(8), assets.smoke(10))
                .sizeOverLife(Interpolation.pow2Out, 1.5f)
                .colorOverLife(Interpolation.linear, thick, thin))
            // 2. Glints — tiny frost sparkles winking on the surface.
            .emitter(e -> e
                .continuous(3f)
                .speed(range(0f, 0.05f)).life(range(0.2f, 0.4f)).size(range(0.05f, 0.09f)).spread(10f)
                .jitter(0.45f, 0f, 0.25f)
                .texture(assets.star(4))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .color(new Color(0.95f, 1f, 1f, 1f)))
            .build();
    }

    /** Icy vapor rolling off a beam — soft translucent puffs that bloom and thin out, not glowing embers.
     *  Each puff morphs through the smoke frames over its life; the order is authored right here. */
    public static EffectDef beamIceMist(Color tint, int dir) {
        Color near = new Color(0.85f, 0.92f, 1f, 0.4f);       // pale icy white, translucent
        Color far  = new Color(tint.r, tint.g, tint.b, 0f);   // thin out toward the element tint, fully faded
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(45f)
                .speed(range(0.15f, 0.6f)).life(range(1.0f, 2.0f)).size(range(0.5f, 1.1f)).spread(180f)
                .drift(-dir * 0.4f, 0.15f, 0f)                // roll back toward the caster + slow rise
                .spin(range(-25f, 25f)).fadeIn(0.3f)          // rolling vapor, not popping puffs
                .alphaBlend()                                 // soft translucent vapor, not additive glow
                .sizeOverLife(Interpolation.pow2Out, 2.0f)    // bloom outward as it dissipates
                .texturesOverLife(
                    assets.smoke(1), assets.smoke(2), assets.smoke(3), assets.smoke(4), assets.smoke(5),
                    assets.smoke(6), assets.smoke(7), assets.smoke(8), assets.smoke(9), assets.smoke(10))
                .colorOverLife(Interpolation.linear, near, far))
            .build();
    }

    /** Gentle green restoration — glow bloom + ground ring, then hearts and motes rising off the body. */
    public static EffectDef heal() {
Color green = new Color(0.5f, 1f, 0.6f, 1f);
        Color heart = new Color(0.6f, 1f, 0.7f, 0.85f);
        return EffectDef.effect()
            // 1. Body glow bloom — centered on the caster's torso, not the feet
            .emitter(e -> e
                .burst(1).speed(0f).life(0.5f).size(1.2f)
                .atBody()
                .texture(assets.light(1))
                .sizeOverLife(Interpolation.pow2Out, 1.3f)
                .color(new Color(0.7f, 1f, 0.8f, 1f)))
            // 2. Ground ring — stays at the feet
            .emitter(e -> e
                .burst(1).speed(0f).life(0.4f).size(0.7f)
                .texture(assets.circle(5))
                .sizeOverLife(Interpolation.pow2Out, 2.0f)
                .color(green))
            // 3. Hearts drifting up across the body
            .emitter(e -> e
                .burst(6, 0.9f)
                .speed(range(0.1f, 0.3f)).life(range(0.8f, 1.2f)).size(range(0.15f, 0.25f)).spread(40f)
                .atBody()
                .jitter(0.35f, 0.3f, 0.1f)
                .drift(0f, 0.5f, 0f)
                .alphaBlend()
                .texture(assets.symbol(1))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .color(heart))
            // 4. Sparkle motes
            .emitter(e -> e
                .burst(14, 1.0f)
                .speed(range(0.1f, 0.4f)).life(range(0.6f, 1.0f)).size(range(0.1f, 0.2f)).spread(60f)
                .atBody()
                .jitter(0.4f, 0.35f, 0.1f)
                .drift(0f, 0.6f, 0f)
                .textures(assets.star(2), assets.star(4), assets.star(6))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, green))
            .build();
    }

    /** Aggressive red-orange surge — flash + shockring intro, then flames/embers that burn
     *  for the whole buff (continuous; the aura's handle stops them when it fades). */
    public static EffectDef powerUp() {
        Color red    = new Color(1f, 0.35f, 0.2f, 1f);
        Color orange = new Color(1f, 0.6f, 0.25f, 1f);
        return EffectDef.effect()
            // 1. Power flash
            .emitter(e -> e
                .burst(1).speed(0f).life(0.15f).size(1.3f)
                .texture(assets.circle(1))
                .sizeOverLife(Interpolation.pow2Out, 1.3f)
                .colorOverLife(Interpolation.linear, Color.WHITE, red))
            // 2. Ground shockring
            .emitter(e -> e
                .burst(1).speed(0f).life(0.35f).size(0.7f)
                .texture(assets.circle(5))
                .sizeOverLife(Interpolation.pow2Out, 2.5f)
                .color(orange))
            // 3. Rising flames — burn while buffed
            .emitter(e -> e
                .continuous(14f)
                .speed(range(0.2f, 0.5f)).life(range(0.4f, 0.7f)).size(range(0.25f, 0.45f)).spread(30f)
                .jitter(0.35f, 0.25f, 0.1f)
                .drift(0f, 1.0f, 0f)
                .textures(
                    assets.flame(1), assets.flame(2), assets.flame(3),
                    assets.flame(4), assets.flame(5), assets.flame(6))
                .sizeOverLife(Interpolation.linear, 1.4f)
                .colorOverLife(Interpolation.linear, orange, red))
            // 4. Ember sparks — burn while buffed
            .emitter(e -> e
                .continuous(8f)
                .speed(range(0.4f, 0.9f)).life(range(0.3f, 0.6f)).size(range(0.08f, 0.16f)).spread(40f)
                .jitter(0.3f, 0.2f, 0.1f)
                .drift(0f, 1.2f, 0f)
                .texture(assets.star(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, orange, red))
            .build();
    }

    /** Arcane purple attunement — one big sigil stamp + swirl veil, then glints while buffed. */
    public static EffectDef magicUp() {
        Color purple = new Color(0.7f, 0.4f, 1f, 1f);
        return EffectDef.effect()
            // 1. Sigil stamp — ONE big pentagram, slowly turning like a live magic circle
            .emitter(e -> e
                .burst(1).speed(0f).life(0.9f).size(1.1f)
                .spin(40f)
                .texture(assets.magic(1))
                .sizeOverLife(Interpolation.pow2Out, 1.4f)
                .color(purple))
            // 2. Swirl veil — turning with the sigil
            .emitter(e -> e
                .burst(2, 0.3f)
                .speed(0f).life(range(0.5f, 0.8f)).size(range(0.8f, 1.1f))
                .jitter(0.15f, 0.2f, 0.05f)
                .spin(range(30f, 60f))
                .alphaBlend()
                .textures(assets.twirl(1), assets.twirl(2), assets.twirl(3))
                .sizeOverLife(Interpolation.pow2Out, 1.5f)
                .color(new Color(0.7f, 0.4f, 1f, 0.4f)))
            // 3. Arcane glints — shimmer while buffed
            .emitter(e -> e
                .continuous(6f)
                .speed(range(0.1f, 0.3f)).life(range(0.5f, 0.9f)).size(range(0.12f, 0.22f)).spread(50f)
                .jitter(0.4f, 0.35f, 0.1f)
                .drift(0f, 0.4f, 0f)
                .textures(assets.magic(3), assets.magic(4), assets.star(9))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, purple))
            // 4. Glow bloom
            .emitter(e -> e
                .burst(1).speed(0f).life(0.5f).size(1.2f)
                .texture(assets.light(1))
                .sizeOverLife(Interpolation.pow2Out, 1.3f)
                .color(purple))
            .build();
    }

    /** Void suction on the zone tile — a contracting swirl, motes streaming in from the four
     *  adjacent tiles (the pull directions) that get swallowed at the center, and a dark heart.
     *  Deliberately restrained: ~27 live particles at steady state. */
    public static EffectDef voidPull() {
        Color purple = new Color(0.55f, 0.3f, 0.85f, 1f);
        float tileW = Battlefield.panelFloorWidth();
        float tileD = Battlefield.panelFloorDepth();
        float avgLife = 1.1f;   // stream life midpoint; drift covers one tile in this time
        EffectDef.Builder fx = EffectDef.effect()
            // 1. Tile swirl — contracts inward, reads as the vortex. Additive so it glows
            // over the dark void sprite instead of disappearing into it.
            .emitter(e -> e
                .continuous(3f)
                .speed(0f).life(range(1.0f, 1.5f)).size(range(1.0f, 1.3f))
                .spin(range(50f, 80f))   // one direction — the vortex actually turns
                .textures(assets.twirl(1), assets.twirl(2), assets.twirl(3))
                .sizeOverLife(Interpolation.pow2In, 0.35f)
                .color(new Color(0.75f, 0.5f, 1f, 0.6f)))
            // 3. Singularity glow — steady bright heart at the center.
            .emitter(e -> e
                .continuous(3f)
                .speed(0f).life(0.9f).size(0.7f)
                .texture(assets.light(1))
                .color(new Color(0.6f, 0.35f, 1f, 0.7f)));
        // 2. Inward streams — one emitter per pull direction; spawn a tile out, drift to center,
        //    shrink to nothing on arrival (swallowed).
        float[][] from = { {tileW, 0f}, {-tileW, 0f}, {0f, tileD}, {0f, -tileD} };
        for (float[] o : from) {
            fx.emitter(e -> e
                .continuous(8f)
                .speed(0f).life(range(0.9f, 1.3f)).size(range(0.12f, 0.22f))
                .offset(o[0], 0f, o[1])
                .jitter(0.15f, 0.1f, 0.1f)
                .drift(-o[0] / avgLife, 0f, -o[1] / avgLife)
                .texture(assets.star(5))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, purple));   // bright at spawn → corrupted as it's drawn in
        }
        return fx.build();
    }

    /** Slight persistent glow while REGEN ticks — a dim breathing halo + a few rising motes.
     *  Deliberately quieter than heal's cast bloom. */
    public static EffectDef regen() {
        Color green = new Color(0.5f, 1f, 0.6f, 1f);
        return EffectDef.effect()
            // 1. Body glow — overlapping soft copies read as a steady halo, riding the caster.
            .emitter(e -> e
                .continuous(3f)
                .followAnchor()
                .speed(0f).life(0.8f).size(0.85f)
                .atBody()
                .texture(assets.light(1))
                .sizeOverLife(Interpolation.pow2Out, 1.15f)
                .color(new Color(0.6f, 1f, 0.7f, 0.5f)))
            // 2. Rising motes
            .emitter(e -> e
                .continuous(3f)
                .speed(range(0.05f, 0.2f)).life(range(0.7f, 1.1f)).size(range(0.08f, 0.15f)).spread(40f)
                .atBody()
                .jitter(0.3f, 0.25f, 0.1f)
                .drift(0f, 0.4f, 0f)
                .textures(assets.star(2), assets.star(6))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, green))
            .build();
    }

    // ── staging-menu effects (screen-space engine; x/z are stage coords, y is rise) ──

    /** Element-tinted spark puff when a card lands in a slot — spark()'s
     *  little sibling, sized for a 0.66-unit card. Pair with an
     *  {@code Anchor.rim} so the sparks release from the card's frame. */
    public static EffectDef menuAssignBurst(Element element) {
        Color tint = tint(element);
        return EffectDef.effect()
            .emitter(e -> e
                .burst(20, 0.05f)
                .speed(range(0.8f, 1.8f)).life(range(0.3f, 0.6f)).size(range(0.15f, 0.3f)).spread(180f)
                .textures(
                    assets.spotlightA(1), assets.spotlightA(2), assets.spotlightA(3), assets.spotlightA(4),
                    assets.spotlightA(5), assets.spotlightA(6), assets.spotlightA(7), assets.spotlightA(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    /** Sparse cyan motes drifting up while the staging menu is open — atmosphere,
     *  not a fountain; pair with a wide region anchor. */
    public static EffectDef menuMotes() {
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(8f)
                .speed(range(0.02f, 0.1f)).life(range(2f, 4f)).size(range(0.08f, 0.16f)).spread(180f)
                .drift(0f, 0.3f, 0f)
                .fadeIn(0.3f)
                .textures(assets.star(4), assets.star(5))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .color(new Color(0.5f, 0.85f, 0.95f, 0.8f)))
            .build();
    }

    /** Chill confirm send-off — a few teal wisps rising off a locked-in slot,
     *  corrupting toward soft purple as they fade. Deliberately no flash/ring. */
    public static EffectDef menuConfirmWisp() {
        Color teal   = new Color(0.3f, 0.85f, 0.9f, 0.85f);
        Color purple = new Color(0.7f, 0.45f, 1f, 0f);
        return EffectDef.effect()
            .emitter(e -> e
                .burst(8, 0.12f)
                .speed(range(0.15f, 0.4f)).life(range(0.6f, 1.0f)).size(range(0.15f, 0.28f)).spread(50f)
                .drift(0f, 0.35f, 0f)
                .textures(assets.star(2), assets.star(6), assets.magic(4))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, teal, purple))
            .build();
    }

    /** energyMotes' lazier cousin for the staging menu's detail portrait —
     *  same element-tinted motes, drifting at a fraction of the pace. Wears the
     *  element color for its whole life (just a touch of white at spawn). */
    public static EffectDef menuElementWisps(Element element) {
        Color tint = tint(element);
        Color bright = tint.cpy().lerp(Color.WHITE, 0.25f);
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(16f)
                .speed(range(0.03f, 0.12f)).life(range(0.8f, 1.4f)).size(range(0.09f, 0.2f)).spread(180f)
                .jitter(0.3f, 0.05f, 0.2f)
                .drift(0f, 0.25f, 0f)
                .textures(assets.star(4), assets.star(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, bright, tint))
            .build();
    }

    // ── reward-screen effects (screen-space; sized for the 3.8x4.8 reward cards) ──

    /** Card-landing burst — ring + flare glint + a spray of tinted sparks off the
     *  card frame, with a few sparkles hanging after. impact()'s celebratory cousin:
     *  all the layers, none of the smoke. */
    public static EffectDef rewardRevealBurst(Color tint) {
        return EffectDef.effect()
            .emitter(e -> e
                .burst(8, 0.15f)
                .speed(range(0.2f, 0.6f)).life(range(0.6f, 1.0f)).size(range(0.1f, 0.18f)).spread(180f)
                .drift(0f, 0.4f, 0f)
                .textures(assets.star(1), assets.star(4), assets.star(9))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    /** Livelier selected-card aura — tinted motes shimmering up the card face plus
     *  occasional arcane glints. menuElementWisps with the volume turned up. */
    public static EffectDef rewardSelectWisps(Color tint) {
        Color bright = tint.cpy().lerp(Color.WHITE, 0.35f);
        return EffectDef.effect()
            // 1. Rising motes
            .emitter(e -> e
                .continuous(26f)
                .speed(range(0.05f, 0.2f)).life(range(0.7f, 1.2f)).size(range(0.1f, 0.22f)).spread(180f)
                .jitter(0.4f, 0.1f, 0.25f)
                .drift(0f, 0.45f, 0f)
                .textures(assets.star(4), assets.star(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, bright, tint))
           .build();
    }

    /** Claim burst — the loudest beat on the screen, all of it on the chosen card:
     *  double ring + star flash, a fountain of sparks, a soft twirl veil, and
     *  slow-rising motes as the tail. No screen-wide flash. */
    public static EffectDef rewardConfirmBurst(Color tint) {
        Color bright = tint.cpy().lerp(Color.WHITE, 0.5f);
        return EffectDef.effect()
            // 3. Spark fountain — firework streaks up and out, gravity arcing them back.
            // Jitter spreads the launch points across the card face (the burst anchor is
            // the exact card center so the rings/flash stay put).
            .emitter(e -> e
                .burst(34, 0.08f)
                .speed(range(1.2f, 3.0f)).life(range(0.45f, 0.9f)).size(range(0.14f, 0.24f)).spread(70f)
                .jitter(1.8f, 0f, 2.2f)
                .accel(0f, -3.5f, 0f)
                .stretch(0.25f)
                .textures(
                    assets.spotlightA(1), assets.spotlightA(2), assets.spotlightA(3), assets.spotlightA(4),
                    assets.spotlightA(5), assets.spotlightA(6), assets.spotlightA(7), assets.spotlightA(8))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            // 5. Rising tail motes
            .emitter(e -> e
                .burst(10, 0.5f)
                .speed(range(0.1f, 0.3f)).life(range(0.8f, 1.3f)).size(range(0.1f, 0.2f)).spread(60f)
                .jitter(1.1f, 0f, 1.4f)
                .drift(0f, 0.6f, 0f)
                .textures(assets.star(2), assets.star(6))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, bright, tint))
            .build();
    }

    // Reveal-ceremony timings/counts — burst windows here must match the choreography
    // delays in RewardScreen and the totalSpawns fed to the stateful anchors.
    public static final float REWARD_GATHER_TIME = 0.6f;
    public static final int   REWARD_GATHER_SPAWNS = 170;
    public static final int   REWARD_GATHER_GLYPHS = 10;
    /** Streaks hold until the magic circle hits peak brightness, then launch off its rim. */
    public static final float REWARD_GATHER_DELAY = 0.35f;
    public static final float REWARD_CIRCLE_RADIUS = 0.6f;

    /** Reward-screen backdrop — a magic-system void: drifting blue-white dust plus
     *  faint arcane runes rising slowly. Play over the full-screen region; builds up
     *  over the first seconds (no prewarm — the gather ceremony covers the cold start). */
    public static EffectDef rewardAmbience() {
        return EffectDef.effect()
            // 1. Dust field
            .emitter(e -> e
                .continuous(14f)
                .speed(range(0.02f, 0.12f)).life(range(2.5f, 5f)).size(range(0.05f, 0.14f)).spread(180f)
                .drift(0f, 0.12f, 0f)
                .fadeIn(0.3f)
                .textures(assets.light(1), assets.light(2), assets.light(3))
                .sizeOverLife(Interpolation.pow2In, 0f)
                .color(new Color(0.55f, 0.7f, 1f, 0.5f)))
            // 2. Faint runes drifting up, turning almost imperceptibly
            .emitter(e -> e
                .continuous(1.2f)
                .speed(range(0.02f, 0.06f)).life(range(3f, 6f)).size(range(0.35f, 0.6f))
                .drift(0f, 0.15f, 0f)
                .spin(range(-12f, 12f)).fadeIn(0.25f)
                .textures(assets.magic(3), assets.magic(4))
                .color(new Color(0.45f, 0.65f, 1f, 0.28f)))
            .build();
    }

    /** One arm of the reveal vortex — pair with {@link Anchor#spiralIn} running outward;
     *  the spawn point does the swirling, and each star inherits its motion so it flies
     *  along the spiral path (drag reins it in) as the energy unwinds from the magic circle
     *  out to the card borders. Single emitter by design: the anchor is stateful, one step
     *  per spawn. */
    public static EffectDef rewardGatherArm(Color tint) {
        Color bright = tint.cpy().lerp(Color.WHITE, 0.4f);
        return EffectDef.effect()
            .emitter(e -> e
                .burst(REWARD_GATHER_SPAWNS, REWARD_GATHER_TIME).delay(REWARD_GATHER_DELAY)
                .speed(range(0.02f, 0.08f)).life(range(0.35f, 0.6f)).size(of(0.13f))
                .inherit(4f)
                .textures(assets.star(8))
                .colorOverLife(Interpolation.linear, bright, tint))
            .build();
    }

    /** Magic circle the reveal casts from — fades in and swells to peak brightness right
     *  as the streaks launch off its rim (REWARD_GATHER_DELAY), then fades away, gone as
     *  the last few streaks leave. Pair with {@link Anchor#at} on the card center. */
    public static EffectDef rewardGatherCircle(Color tint) {
        return EffectDef.effect()
            // Pops in a beat before the streaks launch: full alpha at spawn, oversized and
            // slamming down to rest size almost instantly (exp10Out over the whole life =
            // stamp in the first ~0.1s). pow2In then drops the alpha fast — dark before the
            // last streaks finish spawning, never outliving them.
            // Two identical copies: additive stacking doubles the peak brightness.
            .emitter(e -> e
                .burst(1).delay(0.25f).speed(0f).life(0.7f).size(REWARD_CIRCLE_RADIUS * 3f)
                .alphaOverLife(Interpolation.pow2In)
                .sizeOverLife(Interpolation.exp10Out, 0.67f)
                .texture(assets.magicA(2))
                .color(tint))
            .emitter(e -> e
                .burst(1).delay(0.25f).speed(0f).life(0.7f).size(REWARD_CIRCLE_RADIUS * 3f)
                .alphaOverLife(Interpolation.pow2In)
                .sizeOverLife(Interpolation.exp10Out, 0.67f)
                .texture(assets.magicA(2))
                .color(tint))
            .build();
    }

    /** Arcane radicals riding the gather spiral — sparse rune glyphs swept toward the
     *  center among the mote arms. Single emitter by design: pair with its own
     *  {@link Anchor#spiralIn} instance. */
    public static EffectDef rewardGatherGlyphs(Color tint) {
        Color bright = tint.cpy().lerp(Color.WHITE, 0.5f);
        return EffectDef.effect()
            .emitter(e -> e
                .burst(REWARD_GATHER_GLYPHS, REWARD_GATHER_TIME).delay(REWARD_GATHER_DELAY)
                .speed(range(0.02f, 0.06f)).life(range(0.3f, 0.5f)).size(range(0.25f, 0.4f))
                .textures(assets.flare(1))
                .sizeOverLife(Interpolation.pow2In, 0.5f)
                .colorOverLife(Interpolation.linear, bright, new Color(tint.r, tint.g, tint.b, 0.6f)))
            .build();
    }

    public static EffectDef ambientDust() {
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(3f)
                .speed(range(0.02f, 0.20f)).life(range(4f, 8f)).size(range(0.08f, 0.2f)).spread(180f)
                .drift(0f, 0.15f, 0f)   // gentle upward float
                .fadeIn(0.25f)          // materialize, don't pop — these live for seconds
                .texture(assets.star(5))   // dust mote
                .color(new Color(0.6f, 0.7f, 0.95f, 1f)))
            .build();
    }

    public static Color tint(Element element) {
        return switch (element) {
            case FIRE      -> new Color(1f, 0.5f, 0.15f, 1f);
            case ICE       -> new Color(0.5f, 0.85f, 1f, 1f);
            case POISON    -> new Color(0.55f, 0.9f, 0.3f, 1f);
            case LIGHTNING -> new Color(1f, 0.95f, 0.4f, 1f);
            case DARK      -> new Color(0.6f, 0.4f, 0.9f, 1f);
            case NONE      -> new Color(Color.WHITE);
        };
    }
}
