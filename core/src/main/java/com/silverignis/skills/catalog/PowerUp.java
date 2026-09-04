package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class PowerUp {

    private PowerUp() {}

    public static Skill define() {
        return Skill.builder()
            .id("power_up")
            .displayName("Power Up")
            .description("Triple your physical power for 10 seconds.")
            .icon(SkillCatalog.icon("skills/power_up.png"))
            .shape(Skill.Shape.AURA)
            .element(Element.NONE)
            .cooldown(12.0f)
            .manaCost(1)
            .effect(Effect.applyStatus(StatusType.POWER_UP, 10f, 100, 0))
            .build();
    }
}
