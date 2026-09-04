package com.silverignis.skills.keywords;

import com.silverignis.components.Team;
import com.silverignis.skills.CastInfo;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.slots.SlotKey;
import com.silverignis.systems.SkillVars;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;
import com.silverignis.systems.combat.status.ChillStatus;

public final class Keywords {

    static final String STAGED_TIME = "stagedTime";

    private Keywords() {}

    /** Overheat X: at cast, auto-spend X mana if the caster has it. Player-only —
     *  the pool is the player's. */
    public static boolean overheat(SkillContext ctx, Combatant caster, int cost){
        return caster.getTeam() == Team.PLAYER && ctx.mana.trySpend(cost);
    }

    /** Cultivate X: counts this use; true once the skill has grown. Once true,
     *  always true — the counter only climbs. */
    public static boolean cultivate(SkillContext ctx, Combatant caster, Skill def, int x) {
        return ctx.skillVars.inc(caster, def.getId(), "cultivate") >= x;
    }

    /** Virulence X: the target already carries at least X poison stacks. */
    public static boolean virulence(Combatant target, int x){
        Status poison = target.getStatusContainer().get(StatusType.POISON);
        return poison != null && poison.stacks() >= x;
    }

    /** Surge X: this cast resolved as the Xth skill of a loaded sequence (1-based). */
    public static boolean surge(CastInfo cast, int x) {
        return cast.fromSequence() && cast.sequenceIndex() == x - 1;
    }

    private static final String BARGAIN_SPENT = "bargainSpent";

    /** Bargain X: resolved from a sequence, auto-spend X mana and return the card
     *  to the bottom of the slot it came from. The returned copy never bargains
     *  again — its spent mark is consumed by its next cast, whatever kind. */
    public static boolean bargain(SkillContext ctx, Combatant caster, Skill def, CastInfo cast, int cost) {
        String id = def.getId();
        boolean returnedCopy = ctx.skillVars.get(caster, id, BARGAIN_SPENT, 0) == 1;
        if (returnedCopy) ctx.skillVars.set(caster, id, BARGAIN_SPENT, 0);

        if (returnedCopy || !cast.fromSequence()) return false;
        if (caster.getTeam() != Team.PLAYER || !ctx.mana.trySpend(cost)) return false;

        ctx.skillVars.set(caster, id, BARGAIN_SPENT, 1);
        caster.getCaster().getSlots().get(cast.fromSlot()).addBottom(def);
        return true;
    }

    /** Staging entry: bargain-returned cards don't survive the menu — re-acquiring
     *  the skill goes through the hand's normal cooldown and mana costs. */
    public static void expireBargains(SkillVars vars, Combatant caster) {
        for (SlotKey key : SlotKey.values()) {
            caster.getCaster().getSlots().get(key).removeIf(s -> {
                if (vars.get(caster, s.getId(), BARGAIN_SPENT, 0) != 1) return false;
                vars.set(caster, s.getId(), BARGAIN_SPENT, 0);
                return true;
            });
        }
    }

    /** Overcharge X: the card sat staged and uncast for X+ seconds. Casting
     *  spends the clock either way — take() clears even under the threshold. */
    public static boolean overcharge(SkillContext ctx, Combatant caster, Skill def, float x) {
        return ctx.skillVars.take(caster, def.getId(), STAGED_TIME) >= x;
    }

    public static void tickStagedTime(SkillVars vars, Combatant player, float delta) {
        /** Once per frame from Encounter.tick: every staged card gains bench time. */
        for (SlotKey key : SlotKey.values()) {
            player.getCaster().getSlots().get(key).forEach(
                s -> vars.accrue(player, s.getId(), STAGED_TIME, delta));
        }
    }

    /** Chill X: bank X chill stacks on the caster. Staging spends them to
     *  discount ice cards. */
    public static void chill(SkillContext ctx, Combatant caster, int stacks) {
        if (caster.getTeam() != Team.PLAYER ) return;
        caster.getStatusContainer().apply(new ChillStatus(stacks), ctx.triggerBus);
    }

    /** Staging side: chill stacks currently banked. */
    public static int chillStacks(Combatant combatant) {
        Status chill = combatant.getStatusContainer().get(StatusType.CHILL);
        return chill == null ? 0 : chill.stacks();
    }

    /** Staging commit: spend banked stacks. */
    public static void consumeChill(Combatant combatant, int n) {
        if (n<=0) return;
        Status chill = combatant.getStatusContainer().get(StatusType.CHILL);
        if (chill instanceof ChillStatus cs) cs.consume(n);
    }
}
