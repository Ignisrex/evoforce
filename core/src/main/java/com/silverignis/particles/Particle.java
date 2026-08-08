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
    public float rot;    // degrees
    public float spin;   // degrees/sec
    public float sizeFrom;
    public float sizeEndScale = 1f;
    public Interpolation sizeInterp = Interpolation.linear;
    /** Index into the spec's texture set, picked at spawn. Doubles as the start
     *  frame when age-based cycling arrives (frame = (texIndex + ageOffset) % n). */
    public int texIndex;
    public final Color colorFrom = new Color(1,1,1,1);
    public final Color colorTo = new Color(1,1,1,1);
    public Interpolation colorInterp = Interpolation.linear;

    @Override
    public void reset() {
        pos.setZero();
        vel.setZero();
        age = 0f;
        life = 0f;
        rot = 0f;
        spin = 0f;

        sizeFrom = 0f;
        sizeEndScale = 1f;
        sizeInterp = Interpolation.linear;
        texIndex = 0;
        
        colorFrom.set(1,1,1,1);
        colorTo.set(1,1,1,1);
        colorInterp = Interpolation.linear;
    }
}
