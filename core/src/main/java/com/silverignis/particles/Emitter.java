package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public final class Emitter {

    public enum Mode { CONTINUOUS, BURST }

    private final Anchor anchor;
    private final Mode mode;
    private final float rate;  // particles/sec (CONTINUOUS)
    private final int burstCount; // total (BURST)
    private final float burstWindow;  // seconds to spread the burst over (0 = instant)
    private final Val speed, life, size;
    private final float spreadDeg;   // cone half-angle around +Y
    private final Color color;

    private final Vector3 scratch = new Vector3();
    private float accumulator;
    private int emitted;

    private Emitter(Anchor anchor, Mode mode, float rate, int count, float window, Val speed, Val life, Val size,  float spreadDeg, Color color) {
        this.anchor = anchor;
        this.mode = mode;
        this.rate = rate;
        this.burstCount = count;
        this.burstWindow = window;
        this.speed = speed;
        this.life = life;
        this.size = size;
        this.spreadDeg = spreadDeg;
        this.color = color;
    }

    public static Emitter continuous(Anchor anchor, float rate, Val speed, Val life, Val size, float spreadDeg, Color color) {
        return new Emitter(anchor, Mode.CONTINUOUS, rate, 0, 0, speed, life, size, spreadDeg, color);
    }

    public static Emitter burst(Anchor a, int count, float window, Val speed, Val life, Val size, float spreadDeg, Color color) {
        return new Emitter(a, Mode.BURST, 0, count, window, speed, life, size, spreadDeg, color);
    }

    public boolean update(float dt, ParticleEngine engine){
        int n;
        if( mode == Mode.CONTINUOUS) {
            accumulator += rate * dt;
            n = (int) accumulator;
            accumulator -= n;
        }else if (burstWindow <= 0f) {
            n = burstCount;
            emitted = burstCount;
        } else {
            accumulator += burstCount * (dt / burstWindow);
            n = Math.min((int) accumulator, burstCount - emitted);
            accumulator -= n; emitted += n;
        }
        for ( int i = 0; i < n; i++) spawnOne(engine);
        return mode == Mode.BURST && emitted >= burstCount;
    }

    public void spawnOne(ParticleEngine engine) {
        anchor.point(scratch);
        float sp = speed.sample();
        float polar = MathUtils.random(-spreadDeg, spreadDeg) * MathUtils.degRad; //tilt off +Y
        float az = MathUtils.random(0f, MathUtils.PI2);
        float horiz = sp * MathUtils.sin(polar);
        Particle p = engine.spawn(
            scratch.x, scratch.y, scratch.z,
            horiz * MathUtils.cos(az), sp * MathUtils.cos(polar), horiz * MathUtils.sin(az),
            life.sample());
        p.size = size.sample();
        p.color.set(color);
    }
}
