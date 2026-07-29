package com.silverignis.rewards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.silverignis.particles.Vfx;
import com.silverignis.sessions.GameSession;
import com.silverignis.skills.Skill;
import com.silverignis.ui.RewardCardStyle;
import com.silverignis.ui.UiUtil;

public class SkillRewardOption implements RewardOption{

    private final Skill skill;

    public SkillRewardOption(Skill skill) {
        this.skill = skill;
    }

    public Table buildContents(RewardCardStyle style){
        Table card = UiUtil.newTable();
        card.add(new Image(skill.getIcon())).size(1.1f).padBottom(0.12f).row();
        card.add(new Label(skill.getDisplayName(), style.title)).padBottom(0.05f).row();

        String sub = skill.getElement() + " . " + skill.getShape()
            + " . " + (int) skill.getCooldown() + "s CD . " + skill.getManaCost() + " MP";
        card.add(new Label(sub, style.small)).padBottom(0.12f).row();

        Label desc = new Label(skill.getDescription(), style.body);
        desc.setWrap(true);
        desc.setAlignment(Align.center);
        card.add(desc).growX().top();

        return card;
    }

    public void apply(GameSession session) {
        session.playerProfile.getCaster().getDeck().add(skill);
    }

    public Color accent() {
        return Vfx.tint(skill.getElement());
    }
}
