package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.entities.Player;
import com.silverignis.systems.combat.StatusContainer;
import com.silverignis.systems.combat.StatusType;

import java.util.EnumMap;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

/**
 * Status-effect tray, third row of the top-left HUD cluster: one
 * {@link StatusIndicator} per active status on the player, in
 * {@link StatusType} order. A {@link HorizontalGroup} lays the indicators
 * out; an indicator is shown by being its child. The tray fades in with the
 * first status and fades out — last contents frozen — after the final one
 * expires. {@code refresh(Player)} updates it once per frame.
 */
public class StatusHud extends Group {

    public static final float BAR_HEIGHT = 0.5f;
    public static final float GAP_BELOW_MANA = 0.2f;

    private static final float PAD = 0.1f;
    private static final float FADE_TIME = 0.25f;

    private final BitmapFont font;
    private final Texture[] icons = new Texture[StatusType.values().length];
    private final EnumMap<StatusType, StatusIndicator> indicators = new EnumMap<>(StatusType.class);
    private final HorizontalGroup row = new HorizontalGroup();
    private boolean shown = false;

    public StatusHud(RoundedRectShader shader, Viewport viewport) {
        Bezel tray = new Bezel(shader).fill(UiTheme.SURF_LOW).radius(UiTheme.CORNER_RADIUS);
        tray.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH, BAR_HEIGHT);
        addActor(tray);

        Label.LabelStyle style = new Label.LabelStyle(font = genFont(viewport), Color.WHITE);
        for (StatusType t : StatusType.values()) {
            icons[t.ordinal()] = new Texture(Gdx.files.internal(iconPath(t)));
            indicators.put(t, new StatusIndicator(icons[t.ordinal()], style, t.isBeneficial()));
        }

        row.setBounds(PAD, 0f, LifeBarHud.BAR_WIDTH - 2 * PAD, BAR_HEIGHT);
        row.align(Align.left);
        row.setRound(false);
        addActor(row);

        getColor().a = 0f;
        setVisible(false);
    }

    /** Update from the player's active statuses; call once per frame before the stage acts. */
    public void refresh(Player player) {
        StatusContainer statuses = player.getStatusContainer();
        boolean any = false;
        for (StatusType t : StatusType.values()) any |= statuses.has(t);

        // Rebuild only while occupied — during the fade-out the row keeps its
        // last contents, so the tray fades away populated, not as a bare rect.
        if (any) {
            row.clearChildren();
            for (StatusType t : StatusType.values()) {
                if (!statuses.has(t)) continue;
                StatusIndicator indicator = indicators.get(t);
                indicator.update(statuses.get(t));
                row.addActor(indicator);
            }
        }

        if (any != shown) {
            shown = any;
            clearActions();
            addAction(any ? sequence(visible(true), fadeIn(FADE_TIME))
                          : sequence(fadeOut(FADE_TIME), visible(false)));
        }
    }

    private static BitmapFont genFont(Viewport viewport) {
        FreeTypeFontGenerator mono = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/JetBrainsMono-Bold.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = 12;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = mono.generateFont(p);
        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());
        mono.dispose();
        return font;
    }

    // ponytail: skill icons stand in for status art until dedicated icons exist
    private static String iconPath(StatusType t) {
        return "skills/" + switch (t) {
            case FREEZE -> "frost_trap";
            case BURN -> "fire_blast";
            case POISON -> "venom_bomb";
            case STUN -> "thunder";
            case REGEN -> "regen";
            case SHIELD -> "shield";
            case POWER_UP -> "power_up";
            case MAGIC_UP -> "magic_up";
        } + ".png";
    }

    public void dispose() {
        font.dispose();
        for (Texture t : icons) t.dispose();
    }
}
