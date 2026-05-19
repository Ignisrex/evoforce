package com.silverignis.skills;

import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.skills.instances.*;

public final class SkillFactory {

    private SkillFactory() {}

    public static SkillInstance create(Skill skill, Caster caster, GridPosition pos) {
        switch (skill.getShape()) {
            case STRIKE:     return new StrikeInstance(skill, caster, pos);
            case PROJECTILE: return new ProjectileInstance(skill, caster, pos);
            case BEAM:       return new BeamInstance(skill, caster, pos);
            case AURA:       return new AuraInstance(skill, caster, pos);
            case ZONE:       return new ZoneInstance(skill, caster, pos);
            default:
                throw new IllegalStateException("No SkillInstance mapping for shape: " + skill.getShape());
        }
    }
}
