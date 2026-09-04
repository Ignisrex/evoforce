package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class MagicUp {

    private MagicUp() {}

    public static Skill define() {
        return Skill.builder()
            .id("magic_up")
            .displayName("Magic Up")
            .description("Triple your magical power for 10 seconds.")
            .icon(SkillCatalog.icon("skills/magic_up.png"))
            .shape(Skill.Shape.AURA)
            .element(Element.NONE)
            .cooldown(12.0f)
            .manaCost(1)
            .effect(Effect.applyStatus(StatusType.MAGIC_UP, 10f, 100, 0))
            .build();
    }
}
