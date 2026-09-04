package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.components.Caster;
import com.silverignis.skills.Skill;

import java.util.List;

import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;
import static com.silverignis.ui.UiUtil.genFont;

/**
 * The hold-to-load burst column, one slot-gap right of the basic-attack icon:
 * an open-bottomed translucent tray listing the skills queued in
 * {@link Caster#loadedView()} in fire order (top first). The tray glows cyan
 * and its hint flips to ARMED while the trigger is held, so the player can
 * see they are loading rather than firing.
 *
 * <p>Cards stack like {@link SlotsHud}: the first card is the face and each
 * later one peeks out below it, compressing so every loaded skill stays inside. When
 * the queue empties (the burst fires) the cards fly up to the hint and vanish.
 */
public class LoadedHud extends Group {

    private static final float ANCHOR_X = 0.4f + 4 * (0.8f + 0.2f); // past SlotsHud's 3 columns + BasicAttackHud
    private static final float WIDTH    = 1.0f;
    private static final float TOP_Y    = 1.2f + 0.35f + 0.76f;      // SlotsHud's raised Y card top
    private static final float FADE_H   = 1.0f;
    private static final float HINT_PAD = 0.06f;
    private static final float MARGIN_B = 0.2f;

    private static final float CARD_W = 0.6f, CARD_H = 0.57f, ICON_INSET = 0.06f;
    private static final float SLIVER_MAX = 0.30f, SLIVER_MIN = 0.06f;
    private static final float FLY_TIME = 0.25f;

    private static final Color TRAY_FILL  = UiTheme.withA(UiTheme.SURF_LOW, 0.6f);
    private static final float GLOW_ALPHA = 0.5f;
    private static final float GLOW_LERP  = 10f;

    private final RoundedRectShader shader;
    private final BitmapFont font;
    private final Label.LabelStyle style;
    private final Bezel tray;
    private final Label hint;
    private final Group flying = new Group();
    private final Group cards = new Group();
    private final Color glow = UiTheme.withA(UiTheme.CYAN, 0f);
    private final float cardsTop;
    private List<Caster.LoadedSkill> shown = List.of();

    public LoadedHud(RoundedRectShader shader, Viewport viewport) {
        this.shader = shader;
        setPosition(ANCHOR_X, 0f);

        float worldScale = viewport.getWorldHeight() / Gdx.graphics.getHeight();
        FreeTypeFontGenerator mono = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/JetBrainsMono-Bold.ttf"));
        font = genFont(mono, 13, worldScale);
        mono.dispose();
        style = new Label.LabelStyle(font, Color.WHITE);

        tray = new Bezel(shader).fill(TRAY_FILL).border(UiTheme.OUTLINE_V_60, UiTheme.BORDER)
                .radius(UiTheme.CORNER_RADIUS).fade(FADE_H);
        tray.setBounds(0f, 0f, WIDTH, TOP_Y);
        addActor(tray);

        hint = new Label("HOLD RT", style);
        hint.pack();
        hint.setPosition((WIDTH - hint.getWidth()) / 2f, TOP_Y - HINT_PAD - hint.getHeight());
        addActor(hint);
        cardsTop = hint.getY() - HINT_PAD;

        addActor(flying);
        addActor(cards);
    }

    /** Sync with the caster's loaded queue; call once per frame before the stage acts. */
    public void refresh(Caster caster, boolean armed) {
        glow.a = MathUtils.lerp(glow.a, armed ? GLOW_ALPHA : 0f, Math.min(1f, GLOW_LERP * Gdx.graphics.getDeltaTime()));
        tray.glow(glow.a < 0.01f ? null : glow, UiTheme.GLOW_WIDTH);

        hint.setText(armed ? "ARMED" : "HOLD RT");
        hint.setColor(armed ? UiTheme.CYAN : UiTheme.TEXT_DIM);
        hint.pack();
        hint.setX((WIDTH - hint.getWidth()) / 2f);

        List<Caster.LoadedSkill> loaded = caster.loadedView();
        if (loaded.equals(shown)) return;
        if (loaded.isEmpty()) flyOut(); else rebuild(loaded);
        shown = List.copyOf(loaded);
    }

    /**
     * First card is the face, each later one peeks out below it as a sliver,
     * like {@link SlotsHud}; slivers compress once the stack would overflow.
     * Deeper cards are added first so the face draws on top.
     */
    private void rebuild(List<Caster.LoadedSkill> loaded) {
        cards.clearChildren();
        float[] offsets = UiUtil.stackOffsets(loaded.size(), cardsTop - CARD_H - MARGIN_B, SLIVER_MAX, SLIVER_MIN);
        for (int i = loaded.size() - 1; i >= 0; i--) {
            card(loaded.get(i), cardsTop - CARD_H - offsets[i]);
        }
    }

    /** The burst fired: every card rides up to the hint and vanishes there. */
    private void flyOut() {
        while (cards.hasChildren()) {
            Actor c = cards.getChild(0);
            flying.addActor(c);
            c.addAction(sequence(
                parallel(moveTo(c.getX(), cardsTop, FLY_TIME, Interpolation.pow2In),
                         sequence(delay(FLY_TIME * 0.6f), fadeOut(FLY_TIME * 0.4f))),
                Actions.removeActor()));
        }
    }

    private void card(Caster.LoadedSkill loaded, float y) {
        Group g = new Group();
        g.setPosition((WIDTH - CARD_W) / 2f, y);

        Bezel bezel = new Bezel(shader).fill(UiTheme.CARD).border(UiTheme.OUTLINE_V_60, UiTheme.BORDER)
                .radius(UiTheme.CORNER_RADIUS);
        bezel.setBounds(0f, 0f, CARD_W, CARD_H);
        g.addActor(bezel);

        Skill s = loaded.skill();
        if (s.getIcon() != null) {
            Image icon = new Image(s.getIcon());
            icon.setBounds(ICON_INSET, ICON_INSET, CARD_W - 2 * ICON_INSET, CARD_H - 2 * ICON_INSET);
            g.addActor(icon);
        }

        Label letter = new Label(loaded.from().name(), style);
        letter.setColor(UiTheme.GOLD);
        letter.pack();
        letter.setPosition(CARD_W - 0.05f - letter.getWidth(), 0.14f - letter.getHeight());
        g.addActor(letter);

        cards.addActor(g);
    }

    public void dispose() {
        font.dispose();
    }
}
