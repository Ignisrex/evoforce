package com.silverignis.particles;

import com.badlogic.gdx.graphics.Color;
import com.silverignis.skills.elements.Element;

/** Builds a runtime {@link EffectDef} from the firing skill's element/tint and facing.
 *  The named entries in {@link Vfx#byName} adapt the catalog factories to this shape. */
@FunctionalInterface
public interface VfxFactory {
    EffectDef create(Element element, Color tint, int dir);
}
