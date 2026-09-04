package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.StrikeConfig;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class FlameClaw {

    private FlameClaw() {}

    public static Skill define() {
        return Skill.builder()
            .id("flame_claw")
            .displayName("Flame Claw")
            .description("Dash 1 tile forward and rake the 2 tiles beyond it with fire.")
            .icon(SkillCatalog.icon("skills/flame_claw.png"))
            .shape(Skill.Shape.STRIKE)
            .element(Element.FIRE)
            .cooldown(3.0f)
            .manaCost(1)
            .powerScale(0.6f)
            .shapeConfig(new StrikeConfig(1, 2))
            .effect(Effect.damage(22))
            .effect(Effect.applyStatus(StatusType.COMBUSTION, 0f, 100, 3))
            .build();
    }
}
