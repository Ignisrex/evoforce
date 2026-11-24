package com.silverignis.skills.catalog;

import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;

public final class VoidCollapse {

    private VoidCollapse() {}

    public static Skill define() {
        return Skill.builder()
            .id("void_collapse")
            .displayName("Void Collapse")
            .description("A slow sphere of collapsing void two rows wide.\nDebt 2.")
            .icon(SkillCatalog.icon("skills/void_collapse.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.DARK)
            .cooldown(3f)
            .manaCost(4)
            .debt(2)
            .magicScale(0.8f)
            .shapeConfig(ProjectileConfig.straight(5.0f, 2))
            .effect(Effect.damage(45))
            .build();
    }
}
