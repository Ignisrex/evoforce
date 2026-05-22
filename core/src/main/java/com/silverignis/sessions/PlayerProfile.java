package com.silverignis.sessions;

import com.silverignis.components.Caster;
import com.silverignis.components.Stats;
import com.silverignis.components.Team;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillLibrary;

public class PlayerProfile {

    private final Caster caster;
    private final Stats stats;

    public PlayerProfile(SkillLibrary library){
        this.stats = new Stats(20, 10, 100, 10, 20);
        this.caster = new Caster(Team.PLAYER);

        Skill windSlash = library.get("wind_slash");
        for (Skill s : library.all()){
            if ( s == windSlash) continue;
            caster.getDeck().add(s);
        }
        caster.setBasicAttack(windSlash);
    }

    public Caster getCaster() { return caster; }
    public Stats getStats() { return stats; }
}
