package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool.Poolable;

public final class Particle implements Poolable {
    public final Vector3 pos = new Vector3();
    public final Vector3 vel = new Vector3();
    public float age;
    public float life;
    public float size;
    public final Color color = new Color(1,1,1,1);

    @Override
    public void reset() {
        pos.setZero();
        vel.setZero();
        age = 0f;
        life = 0f;
        size = 0f;
        color.set(1,1,1,1);
    }
}
