package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.ZoneConfig;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class VoidPull {

    private VoidPull() {}

    public static Skill define() {
        return Skill.builder()
            .id("void_pull")
            .displayName("Void Pull")
            .description("Open a dark void one tile ahead. Each second it drags aligned foes one tile inward and saps them with dark energy.")
            .icon(SkillCatalog.icon("skills/void_pull.png"))
            .shape(Skill.Shape.ZONE)
            .element(Element.DARK)
            .cooldown(8.0f)
            .manaCost(2)
            .shapeConfig(new ZoneConfig(true, 6.0f, 1.0f))
            .effect(Effect.damage(6))
            .effect(Effect.applyStatus(StatusType.GRAVITY, 6f, 100, 0))
            .build();
    }
}
