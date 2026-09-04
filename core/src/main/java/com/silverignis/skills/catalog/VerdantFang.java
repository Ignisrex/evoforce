package com.silverignis.skills.catalog;

import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.StrikeConfig;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.StrikeInstance;
import com.silverignis.skills.keywords.Keywords;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.event.HealEvent;

public final class VerdantFang extends StrikeInstance {

    private static final int CULTIVATE_X = 3;
    private static final int HEAL_PER_HIT = 6;

    private boolean counted;
    private boolean cultivated;

    public static Skill define() {
        return Skill.builder()
            .id("verdant_fang")
            .displayName("Verdant Fang")
            .description("Lunge and bite with thorned fangs.\nCultivate 3: fully grown fangs sap life from every hit.")
            .icon(SkillCatalog.icon("skills/verdant_fang.png"))
            .shape(Skill.Shape.STRIKE)
            .element(Element.NATURE)
            .cooldown(3.0f)
            .manaCost(1)
            .powerScale(0.8f)
            .shapeConfig(new StrikeConfig(1, 1))
            .effect(Effect.damage(18))
            .instance(VerdantFang::new)
            .build();
    }

    private VerdantFang(Skill def, Combatant caster) { super(def, caster); }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!counted) {
            counted = true;
            cultivated = Keywords.cultivate(ctx, combatant, def, CULTIVATE_X);
        }
        super.update(delta, ctx);
    }

    @Override
    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        super.applyEffectsTo(target, ctx);
        if (cultivated) ctx.damageSystem.heal(new HealEvent(combatant, HEAL_PER_HIT));
    }
}
