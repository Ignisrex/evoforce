package com.silverignis.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.silverignis.systems.combat.Status;

/**
 * One status-effect readout in the {@link StatusHud} tray: icon with the
 * remaining-time text underneath. The text is tinted by the status's
 * polarity — {@link UiTheme#BENEFIT} green for buffs, {@link UiTheme#DETRIMENT}
 * red for debuffs.
 */
public class StatusIndicator extends Group {

    static final float WIDTH     = 0.4f;
    static final float ICON_SIZE = 0.3f;
    static final float TEXT_H    = 0.11f;
    static final float HEIGHT    = TEXT_H + ICON_SIZE;

    private final Label timer;

    StatusIndicator(Texture icon, Label.LabelStyle style, boolean beneficial) {
        setSize(WIDTH, HEIGHT); // measured by the tray's HorizontalGroup
        Image image = new Image(icon);
        image.setBounds((WIDTH - ICON_SIZE) / 2f, TEXT_H, ICON_SIZE, ICON_SIZE);
        addActor(image);

        timer = new Label("", style);
        timer.setColor(beneficial ? UiTheme.BENEFIT : UiTheme.DETRIMENT);
        addActor(timer);
    }

    void update(Status s) {
        timer.setText(String.format("%.1fs", Math.max(0f, s.getRemaining())));
        timer.pack();
        timer.setPosition((WIDTH - timer.getWidth()) / 2f, 0f);
    }
}
