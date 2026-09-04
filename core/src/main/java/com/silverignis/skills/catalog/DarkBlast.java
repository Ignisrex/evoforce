package com.silverignis.skills.catalog;

import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;

public final class DarkBlast {

    private DarkBlast() {}

    public static Skill define() {
        return Skill.builder()
            .id("dark_blast")
            .displayName("Dark Blast")
            .description("Hurl a sphere of void energy down the row.")
            .icon(SkillCatalog.icon("skills/dark_blast.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.DARK)
            .cooldown(3.0f)
            .manaCost(1)
            .powerScale(0.1f)
            .magicScale(0.8f)
            .shapeConfig(ProjectileConfig.straight(8.0f))
            .effect(Effect.damage(18))
            .build();
    }
}
