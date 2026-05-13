package com.silverignis.components;

public class HitFlash {

    private static final float FLASH_DURATION   = 0.3f;
    private static final float FLICKER_INTERVAL = 0.05f;

    private float timer = 0f;

    public void flash() { timer = FLASH_DURATION; }

    public void tick(float delta) { timer = Math.max(0f, timer - delta); }

    public boolean isHidden() {
        return timer > 0f && ((int) (timer / FLICKER_INTERVAL)) % 2 == 1;
    }
}
