package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;

public final class Heal {

    private Heal() {}

    public static Skill define() {
        return Skill.builder()
            .id("heal")
            .displayName("Heal")
            .description("Restore a small amount of HP at once.")
            .icon(SkillCatalog.icon("skills/heal.png"))
            .shape(Skill.Shape.AURA)
            .element(Element.NONE)
            .cooldown(2f)
            .manaCost(2)
            .effect(Effect.heal(15))
            .build();
    }
}
