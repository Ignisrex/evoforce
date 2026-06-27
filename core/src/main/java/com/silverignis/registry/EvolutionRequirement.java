package com.silverignis.registry;

public interface EvolutionRequirement {

    boolean isMet(EvolutionContext ctx);

    String describe();
}

