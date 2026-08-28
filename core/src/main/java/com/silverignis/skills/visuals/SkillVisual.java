package com.silverignis.skills.visuals;

import com.silverignis.particles.ParticleEngine;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;

public interface SkillVisual {

    void onTrigger(Trigger trigger, VisualState state, ParticleEngine engine);

    void update(float delta);

    void render(RenderContext rc, VisualState state);

    default RenderLayer layer() { return RenderLayer.BILLBOARD; }

    boolean isDone();
}
