package com.silverignis.skills.catalog;

import com.silverignis.components.Team;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.AuraInstance;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.status.MetabolizeStatus;

public final class SporeDraught extends AuraInstance {

    private static final int METABOLIZE_STACKS = 6;

    public static Skill define() {
        return Skill.builder()
            .id("spore_draught")
            .displayName("Spore Draught")
            .description("Inhale a bloom of digestive spores.\nMetabolize 6: recover 1 Mana per second while stacks last.")
            .icon(SkillCatalog.icon("skills/spore_draught.png"))
            .shape(Skill.Shape.AURA)
            .element(Element.POISON)
            .cooldown(10.0f)
            .manaCost(1)
            .instance(SporeDraught::new)
            .build();
    }

    private SporeDraught(Skill def, Combatant caster) { super(def, caster); }

    @Override
    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        super.applyEffectsTo(target, ctx);
        target.getStatusContainer().apply(
            new MetabolizeStatus(METABOLIZE_STACKS,
                target.getTeam() == Team.PLAYER ? ctx.mana : null),
            ctx.triggerBus);
    }
}
