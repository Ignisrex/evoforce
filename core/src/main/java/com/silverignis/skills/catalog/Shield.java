package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class Shield {

    private Shield() {}

    public static Skill define() {
        return Skill.builder()
            .id("shield")
            .displayName("Shield")
            .description("Block the next incoming hit.")
            .icon(SkillCatalog.icon("skills/shield.png"))
            .shape(Skill.Shape.AURA)
            .element(Element.NONE)
            .cooldown(2f)
            .manaCost(1)
            .effect(Effect.applyStatus(StatusType.SHIELD, 6f, 100, 0))
            .build();
    }
}
