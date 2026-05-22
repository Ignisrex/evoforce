package com.silverignis.components;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class FreePosition {

    private final Vector2 pos = new Vector2();
    private final float speed;
    private final Rectangle bounds;

    public FreePosition(float x, float y, float speed, Rectangle bounds){
        this.pos.set(x,y);
        this.speed = speed;
        this.bounds = bounds;
    }

    public float getX() { return pos.x; }
    public float getY() { return pos.y; }
    public float getSpeed() { return speed; }
    public Rectangle getBounds() { return bounds; }

    public void set(float x, float y) { pos.set(x, y); }
}
