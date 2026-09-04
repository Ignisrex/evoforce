package com.silverignis.skills.catalog;

import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.ProjectileInstance;
import com.silverignis.skills.keywords.Keywords;
import com.silverignis.systems.combat.Combatant;

public final class UmbralWave extends ProjectileInstance {

    private static final int BARGAIN_COST = 2;

    private boolean checked;

    public static Skill define() {
        return Skill.builder()
            .id("umbral_wave")
            .displayName("Umbral Wave")
            .description("Sweep a crescent of living shadow down the row.\nBargain 2: resolved from a sequence, it slinks back into its slot.")
            .icon(SkillCatalog.icon("skills/umbral_wave.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.DARK)
            .cooldown(4.0f)
            .manaCost(1)
            .magicScale(0.7f)
            .shapeConfig(ProjectileConfig.straight(8f))
            .effect(Effect.damage(22))
            .instance(UmbralWave::new)
            .build();
    }

    private UmbralWave(Skill def, Combatant caster) { super(def, caster); }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!checked) {
            checked = true;
            Keywords.bargain(ctx, combatant, def, castInfo, BARGAIN_COST);
        }
        super.update(delta, ctx);
    }
}
