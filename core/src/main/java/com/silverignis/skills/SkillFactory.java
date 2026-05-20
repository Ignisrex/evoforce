package com.silverignis.skills;

import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.skills.instances.*;
import com.silverignis.systems.combat.Combatant;

public final class SkillFactory {

    private SkillFactory() {}

    public static SkillInstance create(Skill skill, Combatant combatant) {
        switch (skill.getShape()) {
            case STRIKE:     return new StrikeInstance(skill, combatant);
            case PROJECTILE: return new ProjectileInstance(skill, combatant);
            case BEAM:       return new BeamInstance(skill, combatant);
            case AURA:       return new AuraInstance(skill, combatant);
            case ZONE:       return new ZoneInstance(skill, combatant);
            default:
                throw new IllegalStateException("No SkillInstance mapping for shape: " + skill.getShape());
        }
    }
}
