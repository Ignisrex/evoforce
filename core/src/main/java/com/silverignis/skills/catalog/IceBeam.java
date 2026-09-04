package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.BeamInstance;
import com.silverignis.skills.keywords.Keywords;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusType;

public final class IceBeam extends BeamInstance {

    private boolean banked;

    private IceBeam(Skill def, Combatant caster) {
        super(def, caster);
    }

    public static Skill define() {
        return Skill.builder()
            .id("ice_beam")
            .displayName("Ice Beam")
            .description("Fire a freezing beam across the row.\nChill 1: banks a stack that discounts the next staged ice card.")
            .icon(SkillCatalog.icon("skills/ice_beam.png"))
            .shape(Skill.Shape.BEAM)
            .element(Element.ICE)
            .cooldown(4.0f)
            .manaCost(2)
            .effect(Effect.damage(25))
            .effect(Effect.applyStatus(StatusType.FREEZE, 0, 100, 1))
            .instance(IceBeam::new)
            .build();
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!banked) {
            banked = true;
            Keywords.chill(ctx, combatant, 1);
        }
        super.update(delta, ctx);
    }
}
