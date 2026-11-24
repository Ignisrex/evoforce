package com.silverignis.skills.catalog;

import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class FireBlast {

    private FireBlast() {}

    public static Skill define() {
        return Skill.builder()
            .id("fire_blast")
            .displayName("Fire Blast")
            .description("Hurl a fireball down the row.")
            .icon(SkillCatalog.icon("skills/fire_blast.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.FIRE)
            .cooldown(1f)
            .manaCost(1)
            .shapeConfig(ProjectileConfig.straight(8.0f))
            .effect(Effect.damage(20))
            .effect(Effect.applyStatus(StatusType.BURN, 5f, 60, 0))
            .build();
    }
}
