package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.ZoneConfig;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class FrostTrap {

    private FrostTrap() {}

    public static Skill define() {
        return Skill.builder()
            .id("frost_trap")
            .displayName("Frost Trap")
            .description("Place a freezing zone on the tile ahead.")
            .icon(SkillCatalog.icon("skills/frost_trap.png"))
            .shape(Skill.Shape.ZONE)
            .element(Element.ICE)
            .cooldown(1f)
            .manaCost(2)
            .shapeConfig(new ZoneConfig(false, 5.0f, 0.33f))
            .effect(Effect.damage(5))
            .effect(Effect.applyStatus(StatusType.FREEZE, 0, 100, 3))
            .build();
    }
}
