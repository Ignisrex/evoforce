package com.silverignis.skills.visuals.strike;

import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

import java.util.ArrayList;
import java.util.List;

public final class VerdantFangVisual extends AbstractSkillVisual {

    private static final Color FANG_GREEN = new Color(0.5f, 0.95f, 0.45f, 1f);

    private final TextureRegion slash = new TextureRegion(SkillVisuals.assets.texture("skills/sprites/slash.png"));
    private final List<SlashFlash> slashes = new ArrayList<>();

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST -> vs.pose.enterAttack();
            case IMPACT -> {
                slashes.add(new SlashFlash(slash, null, FANG_GREEN, vs.impactPos));
                burst(engine, Vfx.spark(vs.element), vs.impactPos);
            }
            default -> {}
        }
    }

    @Override
    public void update(float delta) {
        for (SlashFlash s : slashes) s.update(delta);
        slashes.removeIf(SlashFlash::isDone);
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        for (SlashFlash s : slashes) s.render(rc);
    }

    public boolean isDone() { return hasEnded() && slashes.isEmpty(); }
}
