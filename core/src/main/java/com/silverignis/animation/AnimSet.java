package com.silverignis.animation;

import java.util.EnumMap;
import java.util.Map;

public final class AnimSet {

    private final Map<AnimState, FrameClip> clips = new EnumMap<>(AnimState.class);
    private final FrameClip idle;

    public AnimSet(FrameClip idle){
        if ( idle == null ) throw new IllegalArgumentException("AnimSet requires an IDLE clip");
        this.idle = idle;
        clips.put(AnimState.IDLE, idle);
    }

    public void put(AnimState state, FrameClip clip) {
        if (clip != null) clips.put(state, clip);
    }

    public FrameClip get(AnimState state) {
        FrameClip c = clips.get(state);
        return c != null ? c : idle;
    }
}
