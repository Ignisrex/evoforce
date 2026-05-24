package com.silverignis.render;
/**
 * Draw-order bucket for {@link SceneRenderable}s. Buckets render in enum order;
 * only {@link #BILLBOARD} is depth-sorted within its bucket.
 *
 * <ul>
 *   <li>{@code GROUND} — flat decals at floor level (zone effects, shadows). Drawn first.</li>
 *   <li>{@code BILLBOARD} — entities, projectiles, beams, auras, sprite VFX. Sorted back-to-front by world Z.</li>
 *   <li>{@code OVERLAY} — HP labels, status icons. Drawn last, always on top.</li>
 * </ul>
 */
public enum RenderLayer { GROUND, BILLBOARD, OVERLAY }
