package com.silverignis.skills;

import com.silverignis.entities.Player;
import com.silverignis.skills.instances.*;

public final class SkillFactory {

    private SkillFactory(){}

    public static SkillInstance create(Skill skill, Player caster){
        switch(skill.getShape()){
            case STRIKE: return new StrikeInstance(skill, caster);
            case PROJECTILE: return new ProjectileInstance(skill, caster);
            case BEAM: return new BeamInstance(skill, caster);
            case AURA: return new AuraInstance(skill, caster);
            case ZONE: return new ZoneInstance(skill, caster);
            default:
                throw new IllegalStateException("No SkillInstance mapping for shape: " + skill.getShape());
        }
    }
}
