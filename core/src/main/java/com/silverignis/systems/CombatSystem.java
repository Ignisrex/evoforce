package com.silverignis.systems;

import com.silverignis.entities.ClashEffect;
import com.silverignis.render.WorldRenderer;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.instances.ProjectileInstance;
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
            active.get(i).update(delta);
        }

        resolveProjectileClashes();

        active.removeIf(SkillInstance::isFinished);
    }

    public void tickStatuses(float delta){
        if (ctx.player.isAlive()){
            ctx.player.getStatusContainer().update(delta, ctx.damageSystem, ctx.triggerBus);
        }

        for (var enemy : ctx.enemies){
            if(!enemy.isAlive()) continue;
            enemy.getStatusContainer().update(delta, ctx.damageSystem, ctx.triggerBus);
        }
    }

    private void resolveProjectileClashes() {
        for (int i = 0; i < active.size(); i++) {
            SkillInstance ai = active.get(i);
            if (ai.isFinished() || !(ai instanceof ProjectileInstance)) continue;
            ProjectileInstance a = (ProjectileInstance) ai;
            if (!a.isStraight()) continue;

            for (int j = i + 1; j < active.size(); j++) {
                SkillInstance bi = active.get(j);
                if (bi.isFinished() || !(bi instanceof ProjectileInstance)) continue;
                ProjectileInstance b = (ProjectileInstance) bi;
                if (!b.isStraight()) continue;
                if (a.getCaster().getTeam() == b.getCaster().getTeam()) continue;
                if (a.getRow() != b.getRow()) continue;

                int row = a.getRow();
                float halfW = ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row) * 0.5f;
                if (Math.abs(a.getCenterX() - b.getCenterX()) > halfW) continue;

                spawnClash(a, b);
                a.finish();
                b.finish();
                break;
            }
        }
    }

    private void spawnClash(ProjectileInstance a, ProjectileInstance b) {
        int row = a.getRow();
        float midX = (a.getCenterX() + b.getCenterX()) * 0.5f;
        float midY = ctx.projectedTileWorld(0, row).y;
        float size = ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row);
        ctx.vfx.add(new ClashEffect(ctx.clashTexture, midX, midY, size, ctx.battlefield.floorZ(row)));
    }

    public void submitRenderables(WorldRenderer renderer) {
        for (SkillInstance s : active) {
            if(s.isFinished()) continue;
            renderer.submit(s);
        }
    }

    public boolean hasActive(){
        return !active.isEmpty();
    }

}
