package com.silverignis.skills.catalog;

import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class VenomBomb {

    private VenomBomb() {}

    public static Skill define() {
        return Skill.builder()
            .id("venom_bomb")
            .displayName("Venom Bomb")
            .description("Lob a toxic glob two tiles forward.")
            .icon(SkillCatalog.icon("skills/venom_bomb.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.POISON)
            .cooldown(1f)
            .manaCost(1)
            .shapeConfig(ProjectileConfig.lob(2, 2.0f, 3.0f, 0.5f))
            .effect(Effect.damage(12))
            .effect(Effect.applyStatus(StatusType.POISON, 6f, 100, 3))
            .build();
    }
}
