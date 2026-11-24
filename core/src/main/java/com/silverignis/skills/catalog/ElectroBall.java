package com.silverignis.skills.catalog;

import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class ElectroBall {

    private ElectroBall() {}

    public static Skill define() {
        return Skill.builder()
            .id("electro_ball")
            .displayName("Electro Ball")
            .description("Lob a crackling ball two tiles forward, leaving a stunning electric field.")
            .icon(SkillCatalog.icon("skills/electro_ball.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.LIGHTNING)
            .cooldown(2f)
            .manaCost(2)
            .shapeConfig(ProjectileConfig.lob(2, 2.0f, 3.0f, 1.0f))
            .effect(Effect.damage(10))
            .effect(Effect.applyStatus(StatusType.STUN, 1.5f, 35, 0))
            .effect(Effect.applyStatus(StatusType.CONDUCTIVE, 10f, 100, 0))
            .build();
    }
}
