package com.silverignis.sessions;

import com.silverignis.components.Caster;
import com.silverignis.components.ManaPool;
import com.silverignis.components.Stats;
import com.silverignis.components.Team;
import com.silverignis.skills.SkillLibrary;

import java.util.List;

public class PlayerProfile {

    private final Caster caster;
    private final Stats stats;
    private int progressionLevel;
    private ManaPool mana = new ManaPool();

    public PlayerProfile(SkillLibrary library){
        this.stats = new Stats(20, 10, 100, 10, 20);
        this.caster = new Caster(Team.PLAYER);

        caster.setBasicAttack(library.get("wind_slash"));
        for (String id : List.of("wind_strike", "fire_blast", "heal", "ice_beam", "shield", "electro_ball")) {
            caster.getDeck().add(library.get(id));
        }

        this.progressionLevel = 0;
    }

    public Caster getCaster() { return caster; }
    public Stats getStats() { return stats; }

    public void progressPlayer() { this.progressionLevel++; }
    public int getProgressionLevel() {
        return progressionLevel;
    }
    public ManaPool getMana(){ return mana; }
}
