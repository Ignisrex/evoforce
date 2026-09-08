package com.silverignis.skills.catalog;

import com.silverignis.components.Team;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.LobInstance;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.status.SapStatus;

public final class SeedBomb extends LobInstance {

    private SeedBomb(Skill def, Combatant caster) {
        super(def, caster);
    }

    public static Skill define() {
        return Skill.builder()
            .id("seed_bomb")
            .displayName("Seed Bomb")
            .description("Lob a leeching seed.\nDirect hits apply Sap.")
            .icon(SkillCatalog.icon("skills/seed_bomb.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.NATURE)
            .cooldown(6.0f)
            .manaCost(2)
            .shapeConfig(ProjectileConfig.lob(2, 2.0f, 2.0f, 0.5f))
            .effect(Effect.damage(10))
            .instance(SeedBomb::new)
            .build();
    }

    // Sap rides only the direct landing hit — the landing cloud replays the
    // data effects and can't carry the injected caster reference.
    @Override
    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        super.applyEffectsTo(target, ctx);
        target.getStatusContainer().apply(
            new SapStatus(combatant, target.getTeam() == Team.PLAYER ? ctx.mana : null),
            ctx.triggerBus);
    }
}
