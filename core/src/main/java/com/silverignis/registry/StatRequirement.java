package com.silverignis.registry;

import com.silverignis.components.Stats;

import java.util.Locale;

public class StatRequirement implements EvolutionRequirement{

    public enum StatType {
        POWER, MAGIC, VITALITY, DEFENSE, SPEED;

        public static StatType fromName(String name) {
            for (StatType t : values()) {
                if (t.name().toLowerCase().equals(name)) return t;
            }
            return null;
        }

        public  int read(Stats stats) {
            switch (this) {
                case POWER : return stats.getPower();
                case MAGIC : return stats.getMagic();
                case VITALITY : return stats.getVitality();
                case DEFENSE : return stats.getDefense();
                case SPEED : return stats.getSpeed();
                default : throw new IllegalStateException("unhandled stat " +  this);
            }
        }
    }
    public final StatType stat;
    public final int min;

    public StatRequirement(StatType stat, int min) {
        this.stat = stat;
        this.min = min;
    }

    @Override
    public boolean isMet(EvolutionContext ctx) {
        return stat.read(ctx.stats) >= min;
    }

    public String describe() {
        return stat.name().toLowerCase() + " >= " + min;
    }


}
