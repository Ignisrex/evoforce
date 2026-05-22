package com.silverignis.components;

public enum Direction {
    UP(0, 1),
    DOWN(0, -1),
    LEFT(-1, 0),
    RIGHT(1,0);

    public final int dCol;
    public final int dRow;

    Direction(int dCol, int dRow){
        this.dCol = dCol;
        this.dRow = dRow;
    }
}


