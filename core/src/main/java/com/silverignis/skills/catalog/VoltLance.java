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
import com.silverignis.systems.combat.StatusFactory;
import com.silverignis.systems.combat.StatusType;

public final class VoltLance extends ProjectileInstance {

    private static final float OVERCHARGE_SECONDS = 6f;
    private static final float STUN_SECONDS = 1.5f;

    private boolean checked;
    private boolean overcharged;

    public static Skill define() {
        return Skill.builder()
            .id("volt_lance")
            .displayName("Volt Lance")
            .description("Hurl a crackling spear down the row.\nOvercharge 6s: a brimming lance stuns and electrifies the ground.")
            .icon(SkillCatalog.icon("skills/volt_lance.png"))
            .shape(Skill.Shape.PROJECTILE)
            .element(Element.LIGHTNING)
            .cooldown(4.0f)
            .manaCost(1)
            .magicScale(0.6f)
            .shapeConfig(ProjectileConfig.straight(10f))
            .effect(Effect.damage(20))
            .instance(VoltLance::new)
            .build();
    }

    private VoltLance(Skill def, Combatant caster) { super(def, caster); }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!checked) {
            checked = true;
            overcharged = Keywords.overcharge(ctx, combatant, def, OVERCHARGE_SECONDS);
        }
        super.update(delta, ctx);
    }

    @Override
    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        super.applyEffectsTo(target, ctx);
        if (overcharged) {
            target.getStatusContainer().apply(
                StatusFactory.create(StatusType.STUN, STUN_SECONDS, 0), ctx.triggerBus);
            ctx.battleState.battlefield.setPanel(target.getCol(), target.getRow(),
                Battlefield.PanelType.LIGHTNING);
            fireImpact(Battlefield.floorX(target.getCol()), 0.35f, Battlefield.floorZ(target.getRow()), ctx);
        }
    }
}
