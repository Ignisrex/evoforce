package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool.Poolable;

public final class Particle implements Poolable {
    public final Vector3 pos = new Vector3();
    public final Vector3 vel = new Vector3();
    public float age;
    public float life;
    public float sizeFrom;
    public float sizeEndScale = 1f;
    public Interpolation sizeInterp = Interpolation.linear;
    public final Color colorFrom = new Color(1,1,1,1);
    public final Color colorTo = new Color(1,1,1,1);
    public Interpolation colorInterp = Interpolation.linear;

    @Override
    public void reset() {
        pos.setZero();
        vel.setZero();
        age = 0f;
        life = 0f;

        sizeFrom = 0f;
        sizeEndScale = 1f;
        sizeInterp = Interpolation.linear;
        
        colorFrom.set(1,1,1,1);
        colorTo.set(1,1,1,1);
        colorInterp = Interpolation.linear;
    }
}
