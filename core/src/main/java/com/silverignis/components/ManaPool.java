package com.silverignis.components;

public class ManaPool {

    private int max = 10;
    private float current = 0;
    private float regenRate = 2; //per second

    public ManaPool(){

    }

    public int getMax(){ return this.max;}
    public float getCurrent(){return this.current;}

    public void update(float delta){
        current = Math.min(max, current + regenRate * delta);
    }

    public void drain(){
        this.current = 0;
    }

    public boolean spendMana(int amt){
        if(amt > current) return false;
        current = current - amt;
        return true;
    }

    public void increaseMax(int amt){
        max += amt;
    }

    public void increaseRegenRate(float rate){
        regenRate += rate;
    }

    public float getRegenRate() {return regenRate;}
}
