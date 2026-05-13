package com.silverignis.systems;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.instances.ZoneInstance;

import java.util.ArrayList;
import java.util.List;

public class CombatSystem {

    private final BattleContext ctx;
    private final List<SkillInstance> active = new ArrayList<>();

    public CombatSystem(BattleContext ctx){
        this.ctx = ctx;
    }

    public BattleContext getBattleContext(){ return this.ctx; }

    public void spawn(SkillInstance instance){
        active.add(instance);
    }

    public void update(float delta) {
        // Iterate over a snapshot index so an instance that spawns another
        // mid-update doesn't get double-ticked this frame.
        for (int i = 0, n = active.size(); i < n; i++){
            active.get(i).update(delta, ctx);
        }

        active.removeIf(SkillInstance::isFinished);
    }

    /** Render zone effects that should appear under entities. */
    public void renderUnder(SpriteBatch batch) {
        for (SkillInstance s : active) {
            if (s instanceof ZoneInstance && ((ZoneInstance) s).isRenderUnder()) {
                s.render(batch, ctx);
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (SkillInstance s : active) {
            if (s instanceof ZoneInstance && ((ZoneInstance) s).isRenderUnder()) continue;
            s.render(batch, ctx);
        }
    }

    public boolean hasActive(){
        return !active.isEmpty();
    }

}
