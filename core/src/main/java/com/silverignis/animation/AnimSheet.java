package com.silverignis.animation;

import java.util.List;

public final class AnimSheet {

    public static final class Row {
        public final AnimState state;
        public final int frameCount;
        public final float fps;
        public final boolean loop;

        private Row(AnimState state, int frameCount, float fps, boolean loop) {
            this.state = state;
            this.frameCount = frameCount;
            this.fps = fps;
            this.loop = loop;
        }
    }

    public static Row row(AnimState state, int frameCount, float  fps, boolean loop) {
        return new Row(state, frameCount, fps, loop);
    }

    public static AnimSheet of(Row... rows){
        return new AnimSheet(rows);
    }

    private final List<Row> rows;

    private  AnimSheet(Row... rows) {
        this.rows = List.of(rows);
    }

    public List<Row> rows() { return rows; }
}
