package com.silverignis.components;

public class Stats {

    private int power;
    private int magic;
    private int vitality;
    private int defense;
    private int speed;

    public Stats(int power, int magic, int vitality, int defense, int speed){
        this.power = power;
        this.magic = magic;
        this.vitality = vitality;
        this.defense = defense;
        this.speed = speed;
    }

    public int getPower() {
        return power;
    }

    public int getMagic() {
        return magic;
    }

    public int getVitality() {
        return vitality;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpeed() {
        return speed;
    }
}
