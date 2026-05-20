package com.silverignis.components;

public class Health {

    private int current;
    private int max;

    public Health(int amt){
        this.current = amt;
        this.max = amt;
    }

    public void heal(int amt){
        this.current = Math.min(current + amt, max);
    }

    public void damage(int amt){
        this.current = Math.max(current - amt, 0);
    }

    public int getCurrent(){
        return current;
    }

    public int getMax(){
        return max;
    }

}
