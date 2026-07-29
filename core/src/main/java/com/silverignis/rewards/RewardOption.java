package com.silverignis.rewards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.silverignis.sessions.GameSession;
import com.silverignis.ui.RewardCardStyle;

public interface RewardOption {

    Table buildContents(RewardCardStyle style);

    void apply(GameSession session);

    Color accent();
}
