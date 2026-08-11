package com.silverignis.rewards;

import com.badlogic.gdx.math.MathUtils;
import com.silverignis.components.Caster;
import com.silverignis.sessions.GameSession;
import com.silverignis.skills.Skill;
import com.silverignis.traits.Trait;
import com.silverignis.traits.TraitsContainer;

import java.util.*;

public final class RewardOffer {

    public final String title;
    public final List<RewardOption> options;

    public RewardOffer(String title, List<RewardOption> options){
        this.title = title;
        this.options = options;
    }

    public static RewardOffer skillOffer(GameSession session) {
        Caster caster = session.playerProfile.getCaster();
        List<Skill> pool = new ArrayList<>();
        for (Skill s : session.skills.all()) {
            if (caster.getDeck().contains(s)) continue;
            if (s == caster.getBasicAttack()) continue;
            pool.add(s);
        }
        if (pool.isEmpty()) return null;

        Collections.shuffle(pool, new Random(MathUtils.random.nextLong()));
        List<RewardOption> options = new ArrayList<>();
        for (Skill s : pool.subList(0, Math.min(3, pool.size()))) {
            options.add(new SkillRewardOption(s));
        }
        return new RewardOffer("CHOOSE A SKILL", options);
    }

    public static RewardOffer traitOffer(GameSession session) {
        TraitsContainer owned = session.playerProfile.getCaster().getTraits();
        List<Trait> pool = new ArrayList<>();
        for (Trait t : session.traits.all()) {
            if (!owned.has(t.getId())) pool.add(t);
        }

        if (pool.isEmpty()) return null;
        Collections.shuffle(pool, new Random(MathUtils.random.nextLong()));
        List<RewardOption> options = new ArrayList<>();
        for (Trait t : pool.subList(0, Math.min(3, pool.size()))) {
            options.add(new TraitRewardOption(t));
        }
        return new RewardOffer("CHOOSE A TRAIT", options);
    }
}
