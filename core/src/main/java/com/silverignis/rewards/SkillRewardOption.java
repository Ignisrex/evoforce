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
import com.silverignis.ui.WorldTextLabel;
import com.silverignis.glossary.Glossary;

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
            + " . " + (int) skill.getCooldown() + "T CD . " + skill.getManaCost() + " MP";
        card.add(new Label(sub, style.small)).padBottom(0.12f).row();

        WorldTextLabel desc = new WorldTextLabel(Glossary.markup(description()), style.bodyMarkup, style.body.fontColor);
        desc.setAlignment(Align.center);
        card.add(desc).growX().top();

        return card;
    }

    public String description() { return skill.getDescription(); }

    public void apply(GameSession session) {
        session.playerProfile.getCaster().getDeck().add(skill);
    }

    public Color accent() {
        return Vfx.tint(skill.getElement());
    }
}
