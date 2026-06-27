package com.silverignis.registry;

import com.silverignis.components.Stats;

import java.util.List;

public class Evolution {

    public final Monster to;
    public final List<EvolutionRequirement> requirements;
    public final Stats bonus;

    public Evolution(Monster to, List<EvolutionRequirement> requirements, Stats bonus) {
        this.to = to;
        this.requirements = requirements;
        this.bonus = bonus;
    }

    public boolean isMet(EvolutionContext ctx){
        for (EvolutionRequirement req : this.requirements){
            if(!req.isMet(ctx)) return false;
        }
        return true;
    }
}
