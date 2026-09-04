package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

public final class MiasmaBeam {

    private MiasmaBeam() {}

    public static Skill define() {
        return Skill.builder()
            .id("miasma_beam")
            .displayName("Miasma Beam")
            .description("A ray of creeping rot.\nMiasma: slows the target's recovery — mana regen, or an enemy's cooldowns.")
            .icon(SkillCatalog.icon("skills/miasma_beam.png"))
            .shape(Skill.Shape.BEAM)
            .element(Element.DARK)
            .cooldown(5.0f)
            .manaCost(2)
            .magicScale(0.6f)
            .effect(Effect.damage(12))
            .effect(Effect.applyStatus(StatusType.MIASMA, 6f, 100, 0))
            .build();
    }
}
