package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class Thunder {

    private Thunder() {}

    public static Skill define() {
        return Skill.builder()
            .id("thunder")
            .displayName("Thunder")
            .description("Sear a row-spanning lightning beam.")
            .icon(SkillCatalog.icon("skills/thunder.png"))
            .shape(Skill.Shape.BEAM)
            .element(Element.LIGHTNING)
            .cooldown(2f)
            .manaCost(2)
            .magicScale(0.7f)
            .effect(Effect.damage(28))
            .effect(Effect.applyStatus(StatusType.STUN, 1.0f, 50, 0))
            .build();
    }
}
