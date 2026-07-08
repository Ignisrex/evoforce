package com.silverignis.particles;

@FunctionalInterface
public interface Drive {
    float value();

    Drive FULL = () -> 1f;
}

