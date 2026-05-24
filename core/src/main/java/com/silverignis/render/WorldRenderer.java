package com.silverignis.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public final class WorldRenderer {

    private static final Comparator<SceneRenderable> BACK_TO_FRONT =
        (a, b) -> Float.compare(a.depth(), b.depth());

    private final EnumMap<RenderLayer, List<SceneRenderable>> buckets = new EnumMap<>(RenderLayer.class);

    public WorldRenderer() {
        for(RenderLayer l : RenderLayer.values()){
            buckets.put(l, new ArrayList<>());
        }
    }

    public void submit(SceneRenderable r){
        buckets.get(r.layer()).add(r);
    }

    public void submit(Iterable<? extends SceneRenderable> rs) {
        for (SceneRenderable r : rs) submit(r);
    }

    public void flush(RenderContext rc) {
        for (SceneRenderable r : buckets.get(RenderLayer.GROUND)) r.render(rc);

        List<SceneRenderable> billboards = buckets.get(RenderLayer.BILLBOARD);
        billboards.sort(BACK_TO_FRONT);
        for (SceneRenderable r : billboards) r.render(rc);

        for (SceneRenderable r : buckets.get(RenderLayer.OVERLAY)) r.render(rc);

        for (List<SceneRenderable> b : buckets.values()) b.clear();
    }
}
