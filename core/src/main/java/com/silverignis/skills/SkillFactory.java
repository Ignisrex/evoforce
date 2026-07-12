package com.silverignis.skills;

import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.skills.instances.*;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;

public final class SkillFactory {

    private SkillFactory() {}

    public static SkillInstance create(Skill skill, Combatant combatant, BattleContext ctx) {
        switch (skill.getShape()) {
            case STRIKE:     return new StrikeInstance(skill, combatant, ctx);
            case PROJECTILE: return isLob(skill)
                    ? new LobInstance(skill, combatant, ctx)
                    : new ProjectileInstance(skill, combatant, ctx);
            case BEAM:       return new BeamInstance(skill, combatant, ctx);
            case AURA:       return new AuraInstance(skill, combatant, ctx);
            case ZONE:       return new ZoneInstance(skill, combatant, ctx);
            default:
                throw new IllegalStateException("No SkillInstance mapping for shape: " + skill.getShape());
        }
    }

    private static boolean isLob(Skill skill) {
        return skill.getShapeConfig() instanceof ProjectileConfig
                && ((ProjectileConfig) skill.getShapeConfig()).getMovementType()
                        == ProjectileConfig.MovementType.LOB;
    }
}
