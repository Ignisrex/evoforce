package com.silverignis.skills.catalog;

import com.silverignis.entities.Battlefield;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.ProjectileInstance;
import com.silverignis.skills.keywords.Keywords;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusType;
import com.silverignis.systems.combat.event.DamageEvent;

public final class ToxicSpine extends ProjectileInstance {

    private static final int VIRULENCE_X = 3;
    private static final int BURST_DMG = 18;

    public static Skill define() {
        return Skill.builder()
            .id("toxic_spine")
            .displayName("Toxic Spine")
            .description("Loose a barbed spine down the row.\nVirulence 3: bursts in a target already riddled with poison.")
            .icon(SkillCatalog.icon("skills/toxic_spine.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.POISON)
            .cooldown(3.0f)
            .manaCost(1)
            .shapeConfig(ProjectileConfig.straight(9f))
            .effect(Effect.damage(14))
            .effect(Effect.applyStatus(StatusType.POISON, 0f, 100, 1))
            .instance(ToxicSpine::new)
            .build();
    }

    private ToxicSpine(Skill def, Combatant caster) { super(def, caster); }

    @Override
    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        // Read the stacks the target arrived with, before this hit's own stack lands.
        boolean virulent = Keywords.virulence(target, VIRULENCE_X);
        super.applyEffectsTo(target, ctx);
        if (virulent) {
            ctx.damageSystem.apply(new DamageEvent(combatant, target, BURST_DMG, DamageEvent.Source.SKILL, def));
            fireImpact(Battlefield.floorX(target.getCol()), 0.35f, Battlefield.floorZ(target.getRow()), ctx);
        }
    }
}
