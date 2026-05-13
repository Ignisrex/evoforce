package com.silverignis.skills;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.slots.SkillSlots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue of every skill the player owns this run. Owns the underlying
 * {@link Texture}s for skill icons — call {@link #dispose()} when the
 * battle ends.
 *
 * <p>The "deck" is just {@code all}. {@link #drawHand} is the one
 * non-trivial piece: it builds a hand of skills that are <em>both</em>
 * off cooldown <em>and</em> not currently sitting in a {@link SkillSlots}
 * slot. Anything sitting in a slot is "in the player's pocket" — it
 * shouldn't show up again in the menu until it's fired.
 */
public class SkillLibrary {

    private final Map<String, Skill> byId = new HashMap<>();
    private final List<Skill> all = new ArrayList<>();

    /**
     * The default starter pool. Only Wind Strike has real behavior today;
     * the others are valid skill data that just happens to map to stub
     * instances, which is fine for testing the menu / slot / cooldown flow.
     */
    public static SkillLibrary defaults() {
        SkillLibrary lib = new SkillLibrary();

        lib.add(new Skill(
            "wind_strike", "Wind Strike",
            "Dash forward and slash the tile in front.",
            new Texture("skills/wind_strike.png"),
            Skill.Shape.STRIKE,
            Element.NONE,
            Arrays.asList(Effect.damage(15)),
            2.0f,
            new Texture("effects/slash.png")
        ));

        lib.add(new Skill(
            "fire_blast", "Fire Blast",
            "Hurl a fireball down the row.",
            new Texture("skills/fire_blast.png"),
            Skill.Shape.PROJECTILE,
            Element.FIRE,
            Arrays.asList(Effect.damage(20)),
            3.0f,
            new Texture("effects/fireball.png"),
            ProjectileConfig.straight(8f)
        ));

        lib.add(new Skill(
            "venom_bomb", "Venom Bomb",
            "Lob a toxic glob two tiles forward.",
            new Texture("skills/venom_bomb.png"),
            Skill.Shape.PROJECTILE,
            Element.POISON,
            Arrays.asList(Effect.damage(12)),
            3.5f,
            new Texture("effects/venom_ball.png"),
            ProjectileConfig.lob(2, 2.0f)
        ));

        Texture iceBeamVfx = new Texture("effects/ice_beam_vfx.png");
        Texture iceSheet = new Texture("skills/animations/icebeam_spritesheet.png");
        int frameW = 256;
        int frameH = 128;
        int cols = iceSheet.getWidth() / frameW;
        TextureRegion[][] tmp = TextureRegion.split(iceSheet, frameW, frameH);
        TextureRegion[] iceFrames = new TextureRegion[cols];
        for (int i = 0; i < cols; i++) {
            iceFrames[i] = tmp[0][i];
        }
        Animation<TextureRegion> iceBeamAnim = new Animation<>(0.1f, iceFrames);

        lib.add(new Skill(
            "ice_beam", "Ice Beam",
            "Fire a freezing beam across the row.",
            new Texture("skills/ice_beam.png"),
            Skill.Shape.BEAM,
            Element.ICE,
            Arrays.asList(
                Effect.damage(25),
                new Effect(Effect.Type.FREEZE, 0, 2, 100)
            ),
            4.0f,
            iceBeamVfx,
            iceBeamAnim,
            null
        ));

        lib.add(new Skill(
            "frost_trap", "Frost Trap",
            "Place a freezing zone on the tile ahead.",
            new Texture("skills/frost_trap.png"),
            Skill.Shape.ZONE,
            Element.ICE,
            Arrays.asList(
                Effect.damage(5),
                new Effect(Effect.Type.FREEZE, 0, 2, 100)
            ),
            3.5f,
            new Texture("effects/beam.png")
        ));

        lib.add(new Skill(
            "heal", "Heal",
            "Restore a small amount of HP.",
            new Texture("skills/heal.png"),
            Skill.Shape.AURA,
            Element.NONE,
            Collections.<Effect>emptyList(),
            5.0f,
            new Texture("effects/aura.png")
        ));

        lib.add(new Skill(
            "shield", "Shield",
            "Block the next incoming hit.",
            new Texture("skills/shield.png"),
            Skill.Shape.AURA,
            Element.NONE,
            Collections.<Effect>emptyList(),
            4.0f,
            new Texture("effects/aura.png")
        ));

        return lib;
    }

    public void add(Skill skill) {
        byId.put(skill.getId(), skill);
        all.add(skill);
    }

    public Skill get(String id) {
        return byId.get(id);
    }

    public List<Skill> all() {
        return Collections.unmodifiableList(all);
    }

    /**
     * Draw up to {@code n} skills that are eligible for the staging menu:
     * <ul>
     *   <li>not currently on cooldown, and</li>
     *   <li>not currently sitting in any X/Y/B slot.</li>
     * </ul>
     * If fewer than {@code n} are eligible, returns however many there are.
     */
    public List<Skill> drawHand(int n, SkillCooldowns cooldowns, SkillSlots slots) {
        List<Skill> eligible = new ArrayList<>();
        for (Skill s : all) {
            if (cooldowns.isOnCooldown(s)) continue;
            if (slots != null && slots.contains(s)) continue;
            eligible.add(s);
        }
        Collections.shuffle(eligible, new java.util.Random(MathUtils.random.nextLong()));
        return eligible.subList(0, Math.min(n, eligible.size()));
    }

    public void dispose() {
        for (Skill s : all) {
            if (s.getIcon() != null) s.getIcon().dispose();
            s.getVfxTexture().dispose();
        }
    }
}
