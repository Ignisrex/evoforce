package com.silverignis.animation;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public final class FrameClip {

    private final Array<TextureRegion> frames;
    private final float frameDuration;
    private final boolean loop;

    public FrameClip(Array<TextureRegion> frames, float fps, boolean loop) {
        this.frames = frames;
        this.frameDuration = 1f /fps;
        this.loop = loop;
    }

    public int frameCount() { return frames.size; }
    public boolean loops() { return loop; }
    public float duration() { return frames.size * frameDuration; }

    public TextureRegion frame(float time) {
        if (frames.size == 0) return null;
        int idx = (int) (time / frameDuration );
        if(loop) {
            idx = idx % frames.size;
        }else if (idx >= frames.size ) {
            idx = frames.size - 1;
        }
        return frames.get(idx);
    }
}
