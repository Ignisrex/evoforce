package com.silverignis.registry;

import com.silverignis.components.Stats;

import java.util.List;

public class MonsterDef {

    public final Monster id;
    public final List<Evolution> evolutions;

    private final int power, magic, vitality, defense, speed;

    public MonsterDef(Monster id, int power, int magic, int vitality, int defense, int speed, List<Evolution> evolutions) {
        this.id = id;
        this.power = power;
        this.magic = magic;
        this.vitality = vitality;
        this.defense = defense;
        this.speed = speed;
        this.evolutions = evolutions;
    }

    public Stats newBaseStats() {
        return new Stats(power, magic, vitality, defense, speed);
    }


}
