package com.silverignis.systems.spawn;

import com.silverignis.registry.Monster;

import java.util.Collections;
import java.util.List;

public class SpawnConfig {

    public final Monster species;

    public final List<String> skillSet;
    public final String basicAttack;

    public final int power;
    public final int magic;
    public final int vitality;
    public final int defense;
    public final int speed;

    private SpawnConfig(Builder b) {
        this.species = b.species;
        this.skillSet = b.skillSet != null ? b.skillSet : Collections.emptyList();
        this.basicAttack = b.basicAttack;
        this.power = b.power;
        this.magic = b.magic;
        this.vitality = b.vitality;
        this.defense = b.defense;
        this.speed = b.speed;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Monster species;
        private List<String> skillSet;
        private String basicAttack;
        private int power;
        private int magic;
        private int vitality;
        private int defense;
        private int speed;

        private Builder() {}

        public Builder species(Monster v)        { this.species = v; return this; }
        public Builder skillSet(List<String> v)  { this.skillSet = v; return this; }
        public Builder basicAttack(String v)     { this.basicAttack = v; return this; }
        public Builder power(int v)              { this.power = v; return this; }
        public Builder magic(int v)              { this.magic = v; return this; }
        public Builder vitality(int v)           { this.vitality = v; return this; }
        public Builder defense(int v)            { this.defense = v; return this; }
        public Builder speed(int v)              { this.speed = v; return this; }

        public SpawnConfig build() {
            if (species == null) throw missing("species");
            return new SpawnConfig(this);
        }

        private IllegalStateException missing(String field) {
            String which = species != null ? species.name() : "<unknown>";
            return new IllegalStateException(
                "SpawnConfig '" + which + "' missing required field '" + field + "'");
        }
    }
}
