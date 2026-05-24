package com.silverignis.render;

public interface SceneRenderable {
    /** Scene depth (world Z). For grid-locked things: {@code battlefield.floorZ(row)}. */
    float depth();

    RenderLayer layer();

    void render(RenderContext rc);
}
