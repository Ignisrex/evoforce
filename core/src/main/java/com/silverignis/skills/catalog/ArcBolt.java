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
import com.silverignis.systems.combat.event.DamageEvent;

public final class ArcBolt extends ProjectileInstance {

    private static final int SURGE_POSITION = 3;
    private static final int SURGE_DMG = 15;

    public static Skill define() {
        return Skill.builder()
            .id("arc_bolt")
            .displayName("Arc Bolt")
            .description("Loose a snapping bolt of lightning.\nSurge 3: the bolt calls down a second strike.")
            .icon(SkillCatalog.icon("skills/arc_bolt.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.LIGHTNING)
            .cooldown(3.0f)
            .manaCost(1)
            .magicScale(0.5f)
            .shapeConfig(ProjectileConfig.straight(11f))
            .effect(Effect.damage(15))
            .instance(ArcBolt::new)
            .build();
    }

    private ArcBolt(Skill def, Combatant caster) { super(def, caster); }

    @Override
    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        super.applyEffectsTo(target, ctx);
        if (Keywords.surge(castInfo, SURGE_POSITION)) {
            ctx.damageSystem.apply(new DamageEvent(combatant, target, SURGE_DMG, DamageEvent.Source.SKILL, def));
            fireImpact(Battlefield.floorX(target.getCol()), 0.35f, Battlefield.floorZ(target.getRow()), ctx);
        }
    }
}
