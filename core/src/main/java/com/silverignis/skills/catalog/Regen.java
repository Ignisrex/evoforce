package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class Regen {

    private Regen() {}

    public static Skill define() {
        return Skill.builder()
            .id("regen")
            .displayName("Regen")
            .description("Restore a small amount of HP over time.")
            .icon(SkillCatalog.icon("skills/regen.png"))
            .shape(Skill.Shape.AURA)
            .element(Element.NONE)
            .cooldown(5.0f)
            .manaCost(1)
            .effect(Effect.applyStatus(StatusType.REGEN, 4f, 100, 6))
            .build();
    }
}
