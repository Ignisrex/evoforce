package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;

public final class WindStrike {

    private WindStrike() {}

    public static Skill define() {
        return Skill.builder()
            .id("wind_strike")
            .displayName("Wind Strike")
            .description("Dash forward and slash the tile in front.")
            .icon(SkillCatalog.icon("skills/wind_strike.png"))
            .shape(Skill.Shape.STRIKE)
            .element(Element.NONE)
            .cooldown(1f)
            .manaCost(1)
            .powerScale(0.5f)
            .effect(Effect.damage(30))
            .build();
    }
}
