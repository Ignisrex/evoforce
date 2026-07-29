package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.assets.GeneratedAssets;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Channel;
import com.silverignis.particles.Drive;
import com.silverignis.particles.Emitter;
import com.silverignis.particles.EmitterHandle;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;
import com.silverignis.skills.Skill;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.effects.EffectType;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.skills.slots.SlotKey;

import java.util.List;

import static com.silverignis.ui.UiUtil.genFont;
import static com.silverignis.ui.UiUtil.newTable;

/**
 * Cyber-tactical "Neural Link OS" staging screen (skill_select_inspo.png):
 * glass panels with cyan borders + corner brackets, gold for the selected
 * item, JetBrains Mono / Space Grotesk type. Built on a Scene2D {@link Stage};
 * panel interiors are laid out with {@link Table}, while the gliding cursor,
 * card cascade/pop groups, and slot stacking stay hand-positioned. The
 * center stays clear so the frozen battlefield reads behind. Presentational
 * only — cursor/zone logic lives in {@code SkillSelectState}, pushed in each
 * frame via {@link #refresh}.
 */
public class SkillSelectOverlay {

    // ── palette + shared metrics (single-sourced in UiTheme) ────────────────
    private static final Color CYAN      = UiTheme.CYAN;
    private static final Color CYAN_HI   = UiTheme.CYAN_HI;
    private static final Color GOLD      = UiTheme.GOLD;
    private static final Color TEXT      = UiTheme.TEXT;
    private static final Color TEXT_DIM  = UiTheme.TEXT_DIM;
    private static final Color OUTLINE   = UiTheme.OUTLINE;
    private static final Color OUTLINE_V = UiTheme.OUTLINE_V;
    private static final Color PANEL     = UiTheme.PANEL;
    private static final Color CARD      = UiTheme.CARD;
    private static final Color CARD_HI   = UiTheme.CARD_HI;
    private static final Color SURF_LOW  = UiTheme.SURF_LOW;
    private static final Color STATBOX   = UiTheme.STATBOX;
    private static final Color DIM       = UiTheme.DIM;

    private static final Color CYAN_45      = UiTheme.CYAN_45;
    private static final Color OUTLINE_80   = UiTheme.OUTLINE_80;
    private static final Color OUTLINE_60   = UiTheme.OUTLINE_60;
    private static final Color OUTLINE_V_60 = UiTheme.OUTLINE_V_60;
    private static final Color CARD_70      = UiTheme.CARD_70;

    private static final float BORDER        = UiTheme.BORDER;
    private static final float BORDER_THIN   = UiTheme.BORDER_THIN;
    private static final float BORDER_HEAVY  = UiTheme.BORDER_HEAVY;
    private static final float CORNER_RADIUS = UiTheme.CORNER_RADIUS;
    private static final float GLOW_WIDTH    = UiTheme.GLOW_WIDTH;
    private static final float CURSOR_GLOW_WIDTH = 0.18f;
    private static final float PANEL_PAD    = 0.28f;
    private static final float GAP          = 0.14f;          // slot stacks + tray chips
    private static final float PULSE_SPEED  = 4f;             // tab + cursor glow

    // ── layout anchors (16x9 world units) ───────────────────────────────────
    private static final float OPERATOR_X = 0.30f, OPERATOR_TOP = 8.7f, OPERATOR_W = 3.75f, OPERATOR_H = 1.35f;
    private static final float OPERATOR_PAD = 0.20f; // tighter than PANEL_PAD so the avatar fills the strip
    private static final float DETAIL_X = 0.30f, DETAIL_Y = 1.95f, DETAIL_W = 3.55f, DETAIL_H = 5.05f;
    private static final float DETAIL_IMAGE_H = 1.7f;
    private static final float STAT_BOX_H = 0.72f, STAT_BOX_GAP = 0.15f;
    private static final float STACKS_CENTER_X = 14.55f, STACKS_Y = 5.0f, SLOT_SIZE = 0.66f;
    private static final float CARD_ICON_INSET = 0.16f; // fraction of SLOT_SIZE
    private static final float TRAY_Y = 0.28f, TRAY_H = 1.62f;
    private static final float TRAY_CENTER_X = 8.0f, TRAY_CHIP_W = 1.05f, TRAY_MAX_W = 9.6f;
    private static final float TAB_X = 15.55f, TAB_Y = 3.9f, TAB_W = 0.45f, TAB_H = 1.2f;

    // ── animation ───────────────────────────────────────────────────────────
    // Card entrance (cascade) + assign (pop), driven by a clock rather than
    // retained Actions since the tree is rebuilt each frame.
    private static final float CASCADE_DUR = 0.30f, CASCADE_STAGGER = 0.05f, CASCADE_RISE = 0.35f;
    private static final float POP_DUR = 0.20f, POP_AMOUNT = 0.28f;
    private static final float POP_IDLE = 999f; // "popped long ago" sentinel
    private static final float CURSOR_INSET = 0.05f, CURSOR_GLIDE_DUR = 0.16f;
    private static final float SHOW_DUR = 0.22f;                    // fade-in on open
    private static final float HIDE_DUR = 0.18f, HIDE_SINK = 0.25f; // fade + sink on close

    private final Viewport viewport;
    private final Texture pixel;
    private final RoundedRectShader roundedRect;
    private final Stage stage;
    private final Group hud = new Group();
    private final Bezel cursorBezel; // persistent so it can glide via Actions
    private final Bezel tuckTab;

    private final BitmapFont titleFont, bodyFont, statFont, labelFont, tinyFont;
    private final Label.LabelStyle titleStyle, bodyStyle, statStyle, labelStyle, tinyStyle;
    private final BezelDrawable glassBg, statBoxBg, avatarBg;

    // Menu particles: screen-space engine drawn between the panels and the cursor.
    private final ParticleEngine particles = new ParticleEngine();
    private final RenderContext particleCtx;
    private EmitterHandle ambientLeft, ambientTray; // continuous motes; live while the menu shows
    private EmitterHandle detailWisps;              // element motes on the highlighted portrait
    private Skill lastHighlighted;

    private float pulseTime;
    private float timeSinceOpen;              // drives the cascade
    private boolean slotSizesUnsynced = true; // first refresh records sizes without popping
    private final float[] popAge;             // seconds since each slot last gained a card
    private final int[] lastSlotSize;         // slot fill last frame, for assign detection
    // Selection-cursor target set during a build pass; drives the gliding highlight.
    private boolean cursorTargetSet;
    private float cursorTargetX, cursorTargetY, cursorTargetW, cursorTargetH;
    private float cursorDestX, cursorDestY, cursorDestW, cursorDestH;
    private boolean cursorNeedsSnap = true;

    public SkillSelectOverlay(GeneratedAssets generated, Viewport viewport, SpriteBatch batch) {
        this.viewport = viewport;
        this.pixel = generated.pixel();
        this.roundedRect = new RoundedRectShader(pixel);
        this.stage = new Stage(viewport, batch);
        this.particleCtx = RenderContext.screenSpace(batch);

        float worldScale = viewport.getWorldHeight() / Gdx.graphics.getHeight();
        FreeTypeFontGenerator grotesk  = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/SpaceGrotesk-Variable.ttf"));
        FreeTypeFontGenerator monoBold = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/JetBrainsMono-Bold.ttf"));
        FreeTypeFontGenerator monoMed  = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/JetBrainsMono-Medium.ttf"));
        titleFont = genFont(grotesk,  30, worldScale);
        bodyFont  = genFont(monoBold, 17, worldScale);
        statFont  = genFont(monoBold, 22, worldScale);
        labelFont = genFont(monoMed,  16, worldScale);
        tinyFont  = genFont(monoBold, 16, worldScale);
        grotesk.dispose(); monoBold.dispose(); monoMed.dispose();

        titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        bodyStyle  = new Label.LabelStyle(bodyFont,  Color.WHITE);
        statStyle  = new Label.LabelStyle(statFont,  Color.WHITE);
        labelStyle = new Label.LabelStyle(labelFont, Color.WHITE);
        tinyStyle  = new Label.LabelStyle(tinyFont,  Color.WHITE);

        glassBg   = new BezelDrawable(roundedRect, PANEL, CYAN_45, BORDER, CORNER_RADIUS,
                                      withA(CYAN, 0.35f), GLOW_WIDTH);
        statBoxBg = new BezelDrawable(roundedRect, STATBOX, OUTLINE_V, BORDER_THIN, CORNER_RADIUS);
        avatarBg  = new BezelDrawable(roundedRect, CARD_HI, CYAN_HI, BORDER, CORNER_RADIUS);

        int slotCount = SlotKey.values().length;
        popAge = new float[slotCount];
        lastSlotSize = new int[slotCount];
        for (int i = 0; i < slotCount; i++) popAge[i] = POP_IDLE;

        stage.addActor(hud);
        stage.addActor(new ParticleLayer());
        cursorBezel = new Bezel(roundedRect).radius(CORNER_RADIUS);
        cursorBezel.setVisible(false);
        stage.addActor(cursorBezel);
        tuckTab = new Bezel(roundedRect).radius(CORNER_RADIUS);
        tuckTab.setBounds(TAB_X, TAB_Y, TAB_W, TAB_H);
        tuckTab.setVisible(false);
        stage.addActor(tuckTab);
    }

    /** Kick the fade-in entrance; called when the staging menu opens. */
    public void onShow() {
        hud.clearActions();
        hud.setPosition(0f, 0f); // undo any exit sink
        hud.getColor().a = 0f;
        hud.addAction(Actions.fadeIn(SHOW_DUR, Interpolation.pow2Out));
        cursorBezel.clearActions();
        cursorBezel.getColor().a = 1f;
        cursorNeedsSnap = true;
        timeSinceOpen = 0f;
        slotSizesUnsynced = true;
        particles.clear(Channel.MENU);
        lastHighlighted = null;
        startAmbientMotes();
    }

    /**
     * Fade-and-sink exit; runs {@code onComplete} once it finishes. The caller
     * must keep ticking {@link #act} (and stop calling {@link #refresh}) until
     * then, so the exit Actions can play out.
     */
    public void onHide(Runnable onComplete) {
        stopMenuEmitters();
        cursorBezel.clearActions();
        cursorBezel.addAction(Actions.fadeOut(HIDE_DUR, Interpolation.pow2In));
        hud.clearActions();
        hud.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.fadeOut(HIDE_DUR, Interpolation.pow2In),
                        Actions.moveBy(0f, -HIDE_SINK, HIDE_DUR, Interpolation.pow2In)),
                Actions.run(onComplete)));
    }

    public void act(float delta) {
        pulseTime += delta;
        timeSinceOpen += delta;
        for (int i = 0; i < popAge.length; i++) popAge[i] += delta;
        particles.update(delta);
        stage.act(delta);
    }

    public void draw() {
        stage.draw();
    }

    // Static content is rebuilt each frame (paused menu); the cursor + entrance
    // are persistent so their Actions animate.
    public void refresh(List<Skill> hand, int handCursor, boolean inSlots, int slotCursor,
                        SkillSlots slots, Skill highlighted, boolean tucked,
                        float mana, int manaMax) {
        if (tucked) {
            hud.setVisible(false);
            cursorBezel.setVisible(false);
            cursorNeedsSnap = true;
            stopMenuEmitters(); // battlefield view — let live motes drain, spawn no more
            float glow = 0.55f + 0.45f * (float) Math.abs(Math.sin(pulseTime * PULSE_SPEED));
            tuckTab.setVisible(true);
            tuckTab.fill(PANEL).border(withA(GOLD, glow), BORDER)
                   .glow(withA(GOLD, glow * 0.6f), CURSOR_GLOW_WIDTH);
            return;
        }
        tuckTab.setVisible(false);
        hud.setVisible(true);
        hud.clearChildren();
        cursorTargetSet = false;
        if (ambientLeft == null) startAmbientMotes(); // back from tucked
        refreshDetailWisps(highlighted);

        rect(hud, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight()).fill(DIM);
        buildOperator(slots, mana, manaMax);
        buildDetail(highlighted);
        buildStacks(slots, inSlots, slotCursor);
        buildTray(hand, handCursor, inSlots);
        updateCursor();
    }

    private void buildOperator(SkillSlots slots, float mana, int manaMax) {
        int used = slots.getSlotsUsed();
        int max = slots.getSlotCapacity();

        Table avatar = newTable();
        avatar.setBackground(avatarBg);

        Table slotsRow = newTable();
        slotsRow.add(label(labelStyle, "SLOTS", TEXT_DIM)).left().expandX().bottom();
        slotsRow.add(label(statStyle, used + "/" + max, CYAN_HI)).right().bottom();

        Table info = newTable();
        info.add(label(labelStyle, "OPERATOR_01", CYAN)).left().expandX().row();
        info.add(new ChargeBar(clamp01(mana / manaMax))).growX().height(0.16f).padTop(0.08f).row();
        info.add(label(tinyStyle, (int) mana + "/" + manaMax + " MP", UiTheme.MANA)).right().padTop(0.04f).row();
        info.add(slotsRow).growX().expandY().bottom();

        Table panel = glassPanel(OPERATOR_X, OPERATOR_TOP - OPERATOR_H, OPERATOR_W, OPERATOR_H, OPERATOR_PAD);
        panel.add(avatar).size(OPERATOR_H - 2 * OPERATOR_PAD).padRight(0.25f);
        panel.add(info).grow();
    }

    private void buildDetail(Skill s) {
        cornerBrackets(DETAIL_X, DETAIL_Y, DETAIL_W, DETAIL_H);
        Table panel = glassPanel(DETAIL_X, DETAIL_Y, DETAIL_W, DETAIL_H, PANEL_PAD);
        panel.top();

        float imageW = DETAIL_W - 2 * PANEL_PAD;
        panel.add(imageBox(s, imageW, DETAIL_IMAGE_H)).size(imageW, DETAIL_IMAGE_H).row();
        if (s == null) return;

        panel.add(label(titleStyle, s.getDisplayName().toUpperCase(), CYAN)).left().padTop(0.10f).row();

        Table meta = newTable();
        meta.add(label(labelStyle, "CD " + String.format("%.1fs", s.getCooldown()), TEXT_DIM)).width(1.4f).left();
        meta.add(label(labelStyle, s.getShape().name(), GOLD)).left().expandX();
        meta.add(label(labelStyle, s.getManaCost() + " MP", UiTheme.MANA)).right();
        panel.add(meta).growX().padTop(0.06f).row();

        panel.add(divider()).growX().height(BORDER_THIN).padTop(0.10f).row();

        Label description = label(bodyStyle, s.getDescription(), TEXT);
        description.setWrap(true);
        description.setAlignment(Align.topLeft);
        panel.add(description).grow().padTop(0.10f).row();

        Table stats = newTable();
        stats.defaults().uniformX().growX().height(STAT_BOX_H);
        stats.add(statBox("DAMAGE", String.valueOf(damageOf(s)))).padRight(STAT_BOX_GAP);
        stats.add(statBox("COOLDOWN", String.format("%.1fs", s.getCooldown())));
        panel.add(stats).growX();
    }

    /** Skill portrait with the element badge overlaid top-right; local coords. */
    private Group imageBox(Skill s, float w, float h) {
        Group box = new Group();
        rect(box, 0f, 0f, w, h).fill(CARD_HI).border(OUTLINE_V, BORDER).radius(CORNER_RADIUS);
        if (s == null) return box;
        if (s.getIcon() != null) {
            float iconSize = h * 0.82f;
            icon(box, s.getIcon(), w / 2f - iconSize / 2f, h / 2f - iconSize / 2f, iconSize, iconSize, 1f);
        }
        Color elementTint = elementColor(s.getElement());
        float badgeSize = 0.42f;
        float bx = w - badgeSize - 0.1f, by = h - badgeSize - 0.1f;
        rect(box, bx, by, badgeSize, badgeSize).fill(SURF_LOW).border(elementTint, BORDER).radius(CORNER_RADIUS);
        placeLabel(box, statStyle, elementLetter(s.getElement()), elementTint,
                   bx + badgeSize / 2f, by + badgeSize - 0.09f, Align.center);
        return box;
    }

    private Table statBox(String name, String value) {
        Table box = newTable();
        box.setBackground(statBoxBg);
        box.pad(0.10f, 0.12f, 0.10f, 0.12f);
        box.add(label(labelStyle, name, TEXT_DIM)).expand().left().top().row();
        box.add(label(statStyle, value, CYAN)).left().bottom();
        return box;
    }

    private void buildStacks(SkillSlots slots, boolean inSlots, int slotCursor) {
        SlotKey[] keys = SlotKey.values();
        int n = keys.length;
        float rowWidth = n * SLOT_SIZE + (n - 1) * GAP;
        float firstX = STACKS_CENTER_X - rowWidth / 2f;

        int used = 0;
        for (SlotKey k : keys) if (!slots.get(k).isEmpty()) used++;
        placeLabel(hud, labelStyle, "STACKS [" + used + "/" + n + "]", TEXT_DIM,
                   STACKS_CENTER_X, STACKS_Y + SLOT_SIZE + 0.35f, Align.center);
        rect(hud, firstX, STACKS_Y + SLOT_SIZE + 0.14f, rowWidth, BORDER_THIN).fill(OUTLINE_V);

        boolean firstSync = slotSizesUnsynced;
        slotSizesUnsynced = false;
        for (int i = 0; i < n; i++) {
            float x = firstX + i * (SLOT_SIZE + GAP);
            ButtonSlot slot = slots.get(keys[i]);
            List<Skill> stacked = slot.view();
            boolean filled = !stacked.isEmpty();

            // Pop the slot when it gains a card since last frame (not on open).
            if (!firstSync && stacked.size() > lastSlotSize[i]) {
                popAge[i] = 0f;
                Vfx.menuAssignBurst(slot.peek().getElement()).play(particles,
                        Anchor.rim(x + SLOT_SIZE / 2f, 0f, STACKS_Y + SLOT_SIZE / 2f,
                                   SLOT_SIZE / 2f, SLOT_SIZE / 2f),
                        Drive.FULL, Channel.MENU);
            }
            lastSlotSize[i] = stacked.size();

            Group card = cardGroup(x, STACKS_Y, SLOT_SIZE / 2f, SLOT_SIZE / 2f, i, popAge[i]);

            // Extra stacked cards stack below the front (local coords), drawn
            // deepest-first so the front card sits on top.
            float iconInset = SLOT_SIZE * CARD_ICON_INSET;
            for (int depth = stacked.size() - 1; depth >= 1; depth--) {
                float stackY = -depth * 0.56f;
                rect(card, 0f, stackY, SLOT_SIZE, SLOT_SIZE).fill(CARD_70).border(OUTLINE_V_60, BORDER).radius(CORNER_RADIUS);
                Skill back = stacked.get(depth);
                if (back.getIcon() != null) {
                    icon(card, back.getIcon(), iconInset, stackY + iconInset,
                         SLOT_SIZE - 2 * iconInset, SLOT_SIZE - 2 * iconInset, 0.75f);
                }
            }

            Bezel face = rect(card, 0f, 0f, SLOT_SIZE, SLOT_SIZE).fill(filled ? CARD : SURF_LOW)
                .border(filled ? GOLD : OUTLINE_V_60, BORDER).radius(CORNER_RADIUS);
            if (filled) face.glow(withA(GOLD, 0.4f), GLOW_WIDTH);
            if (filled && slot.peek().getIcon() != null) {
                icon(card, slot.peek().getIcon(), iconInset, iconInset,
                     SLOT_SIZE - 2 * iconInset, SLOT_SIZE - 2 * iconInset, 1f);
            }
            placeLabel(card, tinyStyle, keys[i].name(), filled ? GOLD : OUTLINE,
                       SLOT_SIZE - 0.06f, 0.16f, Align.right);

            if (inSlots && i == Math.clamp(slotCursor, 0, n - 1)) target(x, STACKS_Y, SLOT_SIZE, SLOT_SIZE);
        }
    }

    /**
     * A card wrapper positioned at ({@code worldX},{@code worldY}) with its
     * children in local coords. Fades + rises in on open (cascade, staggered by
     * {@code index}) and scales up briefly on assign (pop, elapsed {@code popT}).
     */
    private Group cardGroup(float worldX, float worldY, float originX, float originY, int index, float popT) {
        float entrance = Interpolation.pow2Out.apply(
                Math.clamp((timeSinceOpen - index * CASCADE_STAGGER) / CASCADE_DUR, 0f, 1f));
        float scale = popT < POP_DUR ? 1f + POP_AMOUNT * (1f - Interpolation.pow2Out.apply(popT / POP_DUR)) : 1f;
        Group g = new Group();
        g.setTransform(true);
        g.setOrigin(originX, originY);
        g.setScale(scale);
        g.setPosition(worldX, worldY - (1f - entrance) * CASCADE_RISE);
        g.getColor().a = entrance;
        hud.addActor(g);
        return g;
    }

    /** Tray chip geometry for a given hand size (chips squeeze to fit TRAY_MAX_W). */
    private record TrayLayout(float chipW, float chipH, float gap, float trayX, float trayW, float chipY) {
        float chipX(int index) { return trayX + PANEL_PAD + index * (chipW + gap); }
    }

    private static TrayLayout trayLayout(int handSize) {
        int n = Math.max(handSize, 1);
        float chipW = TRAY_CHIP_W, chipH = TRAY_H - 0.24f, gap = GAP;
        float squeeze = Math.min(1f, TRAY_MAX_W / (n * chipW + (n - 1) * gap));
        chipW *= squeeze; gap *= squeeze;
        float trayW = n * chipW + (n - 1) * gap + 2 * PANEL_PAD;
        return new TrayLayout(chipW, chipH, gap,
                TRAY_CENTER_X - trayW / 2f, trayW, TRAY_Y + (TRAY_H - chipH) / 2f);
    }

    private void buildTray(List<Skill> hand, int handCursor, boolean inSlots) {
        TrayLayout t = trayLayout(hand.size());
        glassPanel(t.trayX(), TRAY_Y, t.trayW(), TRAY_H, 0f);
        if (hand.isEmpty()) return;

        for (int i = 0; i < hand.size(); i++) {
            float x = t.chipX(i);
            Group chip = cardGroup(x, t.chipY(), t.chipW() / 2f, t.chipH() / 2f, i, POP_IDLE);
            drawChip(chip, hand.get(i), t.chipW(), t.chipH());
            if (!inSlots && i == handCursor) target(x, t.chipY(), t.chipW(), t.chipH());
        }
    }

    private void drawChip(Group chip, Skill s, float w, float h) {
        rect(chip, 0f, 0f, w, h).fill(CARD).border(OUTLINE_60, BORDER).radius(CORNER_RADIUS);
        placeLabel(chip, tinyStyle, elementLetter(s.getElement()), elementColor(s.getElement()),
                   0.1f, h - 0.12f, Align.left);
        placeLabel(chip, tinyStyle, s.getManaCost() + "MP", UiTheme.MANA, w - 0.1f, h - 0.12f, Align.right);
        placeLabel(chip, tinyStyle, String.valueOf(damageOf(s)), TEXT, w - 0.1f, 0.42f, Align.right);
        if (s.getIcon() != null) {
            float iconSize = h * 0.55f;
            icon(chip, s.getIcon(), w / 2f - iconSize / 2f, h / 2f - iconSize / 2f + 0.05f, iconSize, iconSize, 1f);
        }
        rect(chip, 0.1f, 0.1f, w - 0.2f, 0.06f).fill(CARD_HI);
    }

    private void updateCursor() {
        if (!cursorTargetSet) {
            cursorBezel.setVisible(false);
            cursorNeedsSnap = true;
            return;
        }
        cursorBezel.setVisible(true);
        float glow = 0.6f + 0.4f * (float) Math.abs(Math.sin(pulseTime * PULSE_SPEED));
        cursorBezel.fill(null).border(withA(GOLD, glow), BORDER_HEAVY)
                   .glow(withA(GOLD, glow * 0.6f), CURSOR_GLOW_WIDTH);

        float x = cursorTargetX - CURSOR_INSET, y = cursorTargetY - CURSOR_INSET;
        float w = cursorTargetW + 2 * CURSOR_INSET, h = cursorTargetH + 2 * CURSOR_INSET;
        if (cursorNeedsSnap) {
            cursorBezel.clearActions();
            cursorBezel.setBounds(x, y, w, h);
            cursorNeedsSnap = false;
        } else if (!near(cursorDestX, x) || !near(cursorDestY, y) || !near(cursorDestW, w) || !near(cursorDestH, h)) {
            cursorBezel.clearActions();
            cursorBezel.addAction(Actions.parallel(
                    Actions.moveTo(x, y, CURSOR_GLIDE_DUR, Interpolation.swingOut),
                    Actions.sizeTo(w, h, CURSOR_GLIDE_DUR, Interpolation.swingOut)));
        }
        cursorDestX = x; cursorDestY = y; cursorDestW = w; cursorDestH = h;
    }

    private void target(float x, float y, float w, float h) {
        cursorTargetX = x; cursorTargetY = y; cursorTargetW = w; cursorTargetH = h;
        cursorTargetSet = true;
    }

    // ── menu particles ──────────────────────────────────────────────────────
    /** Chill teal→purple send-off over each filled slot; play before the exit fade. */
    public void confirmFlourish(SkillSlots slots) {
        SlotKey[] keys = SlotKey.values();
        int n = keys.length;
        float firstX = STACKS_CENTER_X - (n * SLOT_SIZE + (n - 1) * GAP) / 2f;
        for (int i = 0; i < n; i++) {
            if (slots.get(keys[i]).isEmpty()) continue;
            Vfx.menuConfirmWisp().play(particles,
                    Anchor.at(firstX + i * (SLOT_SIZE + GAP) + SLOT_SIZE / 2f, 0f, STACKS_Y + SLOT_SIZE / 2f),
                    Drive.FULL, Channel.MENU);
        }
    }

    private void startAmbientMotes() {
        // Two soft bands: the left panel column and the tray strip.
        ambientLeft = Vfx.menuMotes().play(particles,
                Anchor.region(2.2f, 0f, 5.2f, 1.9f, 0f, 3.2f), Drive.FULL, Channel.MENU);
        ambientTray = Vfx.menuMotes().play(particles,
                Anchor.region(TRAY_CENTER_X, 0f, 1.1f, 3.5f, 0f, 0.9f), Drive.FULL, Channel.MENU);
    }

    /** Restart the element motes over the portrait whenever the highlighted card changes. */
    private void refreshDetailWisps(Skill highlighted) {
        if (highlighted == lastHighlighted) return;
        lastHighlighted = highlighted;
        if (detailWisps != null) { detailWisps.stop(); detailWisps = null; }
        if (highlighted == null) return;
        float imageW = DETAIL_W - 2 * PANEL_PAD;
        float imageCenterY = DETAIL_Y + DETAIL_H - PANEL_PAD - DETAIL_IMAGE_H / 2f;
        detailWisps = Vfx.menuElementWisps(highlighted.getElement()).play(particles,
                Anchor.region(DETAIL_X + PANEL_PAD + imageW / 2f, 0f, imageCenterY,
                              imageW * 0.4f, 0f, DETAIL_IMAGE_H * 0.35f),
                Drive.FULL, Channel.MENU);
    }

    private void stopMenuEmitters() {
        if (ambientLeft != null)  { ambientLeft.stop();  ambientLeft = null; }
        if (ambientTray != null)  { ambientTray.stop();  ambientTray = null; }
        if (detailWisps != null)  { detailWisps.stop();  detailWisps = null; }
        lastHighlighted = null;
    }

    // ── actor builders ──────────────────────────────────────────────────────
    private Table glassPanel(float x, float y, float w, float h, float pad) {
        Table panel = newTable();
        panel.setBackground(glassBg);
        panel.setBounds(x, y, w, h);
        panel.pad(pad);
        hud.addActor(panel);
        return panel;
    }

    private void cornerBrackets(float x, float y, float w, float h) {
        float arm = 0.4f, t = BORDER_HEAVY;
        rect(hud, x, y + h - t, arm, t).fill(CYAN);
        rect(hud, x, y + h - arm, t, arm).fill(CYAN);
        rect(hud, x + w - arm, y, arm, t).fill(CYAN);
        rect(hud, x + w - t, y, t, arm).fill(CYAN);
    }

    private Bezel rect(Group g, float x, float y, float w, float h) {
        Bezel b = new Bezel(roundedRect);
        b.setBounds(x, y, w, h);
        g.addActor(b);
        return b;
    }

    private Image divider() {
        Image line = new Image(pixel);
        line.setColor(OUTLINE_V);
        return line;
    }

    private void icon(Group g, Texture tex, float x, float y, float w, float h, float alpha) {
        Image im = new Image(tex);
        im.setBounds(x, y, w, h);
        im.getColor().a = alpha;
        g.addActor(im);
    }

    private Label label(Label.LabelStyle style, String text, Color c) {
        Label l = new Label(text, style);
        l.setColor(c);
        return l;
    }

    /** Hand-placed label; {@code yTop} is the text's top edge, {@code x} honors {@code align}. */
    private Label placeLabel(Group g, Label.LabelStyle style, String text, Color c,
                             float x, float yTop, int align) {
        Label l = label(style, text, c);
        l.pack();
        float lx = align == Align.right ? x - l.getWidth()
                 : align == Align.center ? x - l.getWidth() / 2f : x;
        l.setPosition(lx, yTop - l.getHeight());
        g.addActor(l);
        return l;
    }

    public void dispose() {
        stage.dispose();
        roundedRect.dispose();
        titleFont.dispose(); bodyFont.dispose(); statFont.dispose(); labelFont.dispose(); tinyFont.dispose();
    }

    private static int damageOf(Skill s) {
        int total = 0;
        for (Effect e : s.getEffects()) if (e.getType() == EffectType.DAMAGE) total += e.getValue();
        return total;
    }

    private static String elementLetter(Element e) {
        switch (e) {
            case FIRE: return "F"; case POISON: return "P"; case ICE: return "I";
            case LIGHTNING: return "L"; case DARK: return "D"; default: return "*";
        }
    }

    private static Color elementColor(Element e) {
        switch (e) {
            case FIRE:      return rgb(0xff, 0x8a, 0x3d);
            case POISON:    return rgb(0xa6, 0xe6, 0x59);
            case ICE:       return rgb(0x8c, 0xd9, 0xff);
            case LIGHTNING: return rgb(0xff, 0xe1, 0x6d);
            case DARK:      return rgb(0xbf, 0x8c, 0xff);
            default:        return rgb(0xa6, 0xe6, 0xff);
        }
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 0.001f; }
    private static float clamp01(float v) { return Math.clamp(v, 0f, 1f); }
    private static Color rgb(int r, int g, int b) { return new Color(r / 255f, g / 255f, b / 255f, 1f); }
    private static Color rgba(int r, int g, int b, float a) { return new Color(r / 255f, g / 255f, b / 255f, a); }
    private static Color withA(Color c, float a) { return new Color(c.r, c.g, c.b, a); }

    /** Draws the menu particle engine; sits between the panels and the cursor. */
    private final class ParticleLayer extends Actor {
        @Override
        public void draw(Batch b, float parentAlpha) {
            for (Emitter e : particles.emitters()) e.render(particleCtx);
        }
    }

    /** Charge-meter bar; a ratio-width fill isn't a Table concept, so it lays
     *  out its own Bezels once the cell sizes it. */
    private final class ChargeBar extends Group {
        private final float ratio;

        ChargeBar(float ratio) { this.ratio = ratio; }

        @Override
        protected void sizeChanged() {
            clearChildren();
            float w = getWidth(), h = getHeight();
            // Same radius on all three; the shader clamps it to h/2, so the
            // bar reads as a pill and the fill stays inside the outline.
            rect(this, 0f, 0f, w, h).fill(SURF_LOW).radius(CORNER_RADIUS);
            rect(this, 0f, 0f, w * ratio, h).fill(UiTheme.MANA).radius(CORNER_RADIUS);
            rect(this, 0f, 0f, w, h).border(OUTLINE_80, BORDER_THIN).radius(CORNER_RADIUS);
        }
    }
}
