package com.silverignis.rewards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.silverignis.sessions.GameSession;
import com.silverignis.traits.Trait;
import com.silverignis.ui.RewardCardStyle;
import com.silverignis.ui.UiUtil;

public class TraitRewardOption implements RewardOption {

    private static final Color GOLD = new Color(1f, 0.83f, 0.35f, 1f);
    private final Trait trait;

    public TraitRewardOption(Trait trait) {
        this.trait = trait;
    }

    public Table buildContents(RewardCardStyle style) {
        Table card = UiUtil.newTable();
        card.add(new Image(trait.getIcon())).size(1f).padBottom(0.12f).row();
        card.add(new Label(trait.getDisplayName(), style.title)).padBottom(0.05f).row();
        card.add(new Label("TRAIT . PASSIVE", style.small)).padBottom(0.12f).row();

        Label desc = new Label(trait.getDescription(), style.body);
        desc.setWrap(true);
        desc.setAlignment(Align.center);
        card.add(desc).growX().top();

        return card;
    }

    public void apply(GameSession session) {
        session.playerProfile.getCaster().getTraits().add(trait);
    }

    public Color accent() {
        return Color.GOLD;
    }
}
