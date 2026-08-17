package com.silverignis.skills;

import com.silverignis.skills.instances.*;
import com.silverignis.systems.combat.Combatant;

public final class SkillFactory {

    private SkillFactory() {}

    /** No context: an instance is constructed from its definition and caster
     *  alone, and only meets the simulation when it is first ticked. */
    public static SkillInstance create(Skill skill, Combatant combatant) {
        switch (skill.getShape()) {
            case STRIKE:     return new StrikeInstance(skill, combatant);
            case PROJECTILE: return isLob(skill)
                    ? new LobInstance(skill, combatant)
                    : new ProjectileInstance(skill, combatant);
            case BEAM:       return new BeamInstance(skill, combatant);
            case AURA:       return new AuraInstance(skill, combatant);
            case ZONE:       return new ZoneInstance(skill, combatant);
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
