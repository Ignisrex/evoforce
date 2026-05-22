package com.silverignis.components;

public class GridMovement {

    private final GridPosition position;
    private final GridBounds bounds;

    public GridMovement(GridPosition position, GridBounds bounds){
        this.position = position;
        this.bounds = bounds;
    }

    public GridPosition getPosition() { return position; }
    public GridBounds getBounds() { return bounds; }
}
