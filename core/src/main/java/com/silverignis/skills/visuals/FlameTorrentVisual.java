package com.silverignis.skills.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Flame Torrent: animated stream of fire across the row with flames and
 *  sooty smoke licking off it. */
final class FlameTorrentVisual extends AbstractSkillVisual {

    private final BeamQuad quad = new BeamQuad(looping(), Color.WHITE);

    /** The torrent sheet is a continuous roiled, not a one-shot — it loops. */
    private static Animation<TextureRegion> looping() {
        Animation<TextureRegion> a = SkillVisuals.assets.sheetOnDemand(
            "skills/animations/flame_torrent_spritesheet.png", 256, 128, 0.09f);
        a.setPlayMode(Animation.PlayMode.LOOP);
        return a;
    }

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST     -> vs.pose.enterCast();
            case ACTIVE   -> play(engine, Vfx.beamFlames(vs.dir),
                                  BeamQuad.alongSpan(vs), BeamQuad.intensity(vs));
            case RECOVERY -> vs.pose.enterIdle();
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        quad.draw(rc, vs);
    }
}
