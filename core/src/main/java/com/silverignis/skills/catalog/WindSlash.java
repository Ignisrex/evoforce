package com.silverignis.skills.catalog;

import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;

public final class WindSlash {

    private WindSlash() {}

    public static Skill define() {
        return Skill.builder()
            .id("wind_slash")
            .displayName("Wind Slash")
            .description("Quick wind slash. Always available.")
            .icon(SkillCatalog.icon("skills/sprites/wind_slash.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.NONE)
            .cooldown(0.5f)
            .manaCost(1)
            .shapeConfig(ProjectileConfig.straight(12.0f))
            .effect(Effect.damage(5))
            .build();
    }
}
