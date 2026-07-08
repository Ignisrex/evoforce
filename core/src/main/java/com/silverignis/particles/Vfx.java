package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.silverignis.assets.GameAssets;
import com.silverignis.skills.elements.Element;

import static com.silverignis.particles.Val.of;
import static com.silverignis.particles.Val.range;

public final class Vfx {

    private Vfx() {}

    /** Wired once at startup so the catalog can reference shared VFX textures directly. */
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
                .texture(assets.ember())
                .sizeOverLife(Interpolation.pow2In, 0f)
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    /** Icy vapor rolling off a beam — big, slow, billowing soft puffs instead of sharp embers. */
    public static EffectDef beamMist(Color tint, int dir) {
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(70f)
                .speed(range(0.2f, 1.0f)).life(range(0.7f, 1.4f)).size(range(0.4f, 0.9f)).spread(180f)
                .drift(-dir * 0.5f, 0.25f, 0f)              // roll back toward the caster + slow rise
                .sizeOverLife(Interpolation.linear, 1.7f)   // billow outward as it dissipates
                .texture(assets.mist())
                .colorOverLife(Interpolation.linear, Color.WHITE, tint))
            .build();
    }

    public static EffectDef ambientDust() {
        return EffectDef.effect()
            .emitter(e -> e
                .continuous(3f)
                .speed(range(0.02f, 0.20f)).life(range(4f, 8f)).size(range(0.08f, 0.2f)).spread(180f)
                .drift(0f, 0.15f, 0f)   // gentle upward float
                .texture(assets.dust())
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
