package com.silverignis.skills.catalog;

import com.silverignis.entities.Battlefield;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.BeamInstance;
import com.silverignis.skills.keywords.Keywords;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusType;

public final class FlameTorrent extends BeamInstance {

    private static final int OVERHEAT_COST = 3;

    private boolean checked;
    private boolean overheated;

    private FlameTorrent(Skill def, Combatant caster) {
        super(def, caster);
    }

    public static Skill define() {
        return Skill.builder()
            .id("flame_torrent")
            .displayName("Flame Torrent")
            .description("Unleash a roaring stream of fire across the row.\nOverheat 3: the scorched panels ignite.")
            .icon(SkillCatalog.icon("skills/flame_torrent.png"))
            .shape(Skill.Shape.BEAM)
            .element(Element.FIRE)
            .cooldown(5.0f)
            .manaCost(2)
            .magicScale(0.6f)
            .effect(Effect.damage(16))
            .effect(Effect.applyStatus(StatusType.BURN, 4f, 15, 0))
            .instance(FlameTorrent::new)
            .build();
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!checked) {
            checked = true;
            overheated = Keywords.overheat(ctx, combatant, OVERHEAT_COST);
        }
        super.update(delta, ctx);
    }

    @Override
    protected void applyHit(SkillContext ctx){
        super.applyHit(ctx);
        if (overheated) coveredTiles((c, r) ->
            ctx.battleState.battlefield.setPanel(c, r, Battlefield.PanelType.FIRE));
    }
}
