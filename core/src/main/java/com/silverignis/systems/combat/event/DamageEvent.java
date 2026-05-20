package com.silverignis.systems.combat.event;

import com.silverignis.skills.Skill;
import com.silverignis.systems.combat.Combatant;

public final class DamageEvent {

    public enum Source {SKILL, STATUS, PANEL, UNKNOWN }

    public final Combatant source;
    public final Combatant target;
    public final Source sourceTag;
    public final Skill originalSkill;
    public int amount;

    public DamageEvent(Combatant source, Combatant target, int amount, Source sourceTag, Skill originalSkill){
        this.source = source;
        this.target = target;
        this.amount = amount;
        this.sourceTag = sourceTag;
        this.originalSkill = originalSkill;
    }
}
