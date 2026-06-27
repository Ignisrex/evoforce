package com.silverignis.registry;

import com.silverignis.components.Stats;

public class EvolutionContext {

    public final Stats stats;
    public final int progressionLevel;

    public EvolutionContext(Stats stats, int progressionLevel) {
        this.stats = stats;
        this.progressionLevel = progressionLevel;
    }
}


