# Skill Visuals — design & migration plan

**Date:** 2026-08-25
**Status:** approved design, pending implementation (walkthrough)

## Goal

Skills define their visuals as a named, composable unit instead of six
shape-dependent JSON fields interpreted inconsistently by each shape instance.
A skill's look can layer any of the primitives (sprite, spritesheet animation,
shader quad, particle effects) at any trigger moment (cast, per phase, impact,
clash), authored in Java with full language power.

## Decisions (settled in brainstorming)

1. **Full visual takeover.** Shape instances become gameplay-only: movement,
   hit timing, phase machine. All drawing moves into `SkillVisual` objects.
2. **Code-first, per-skill classes.** Each skill gets its own class
   (`IceBeamVisual`, `DarkBlastVisual`) implementing `SkillVisual`. No generic
   parameterized family classes — shared drawing code lives in *helpers* the
   per-skill classes call (extracted only when duplication is real), never in
   families the skills are instances of.
3. **Visual owns it all.** Textures, sheets, tints, shader ids, particle
   names live in the visual class. `skills.json` carries only an optional
   `"visual"` name; omitted, it defaults to the skill id. The six vfx JSON
   fields die.
4. **Registry pattern** mirroring `Vfx.CATALOG`: `SkillVisuals` maps name →
   `Supplier<SkillVisual>`; unknown names fail at load, one fresh visual
   instance per cast.
5. **Visual lifetime can exceed gameplay.** `SkillInstance.isResolved()` =
   gameplay done (locks released, effects applied — today's `finished`);
   `isFinished()` = `isResolved() && visual.isDone()`, used only for pruning.
   Gameplay queries (clash, covered tiles, hasActive) use `isResolved()`.
6. **Scope in:** caster pose triggers (enterAttack/enterCast move into
   visuals), strike's slash one-shots (replaces `ClashEffect` for skills),
   lob's landing cloud, per-projectile CLASH reactions (replaces the
   hardcoded grey `Vfx.impact` in `CombatSystem`).
   **Scope out (fast-follow):** status visuals (burn etc.).

## Core contract

New package `com.silverignis.skills.visuals`.

```java
public interface SkillVisual {
    void onTrigger(Trigger t, VisualState s, ParticleEngine engine);
    void update(float delta);                    // internal clocks / one-shots
    void render(RenderContext rc, VisualState s);
    RenderLayer layer();                         // BILLBOARD default; GROUND for zone looks
    boolean isDone();                            // may outlive gameplay
}

public enum Trigger { CAST, WINDUP, ACTIVE, RECOVERY, IMPACT, CLASH, END }
```

Universal phase mapping (all shapes fit): Beam CHARGE/FIRE/FADE, Zone
APPEAR/ACTIVE/FADE, Strike DASH/HIT/DASH_BACK, Aura EXPAND/ACTIVE/FADE →
WINDUP/ACTIVE/RECOVERY. Projectile/lob flight is ACTIVE only. Phase entries
fire as triggers; IMPACT may fire multiple times (strike tiles); END always
fires last (stops emitters).

`VisualState` — mutable struct the gameplay instance owns and writes; the
visual only reads. Grid-world space throughout, never screen space.

```java
public final class VisualState {
    // anchors (updated every tick)
    public final Vector3 casterPos = new Vector3();  // caster feet, follows mid-dash
    public final Vector3 bodyPos   = new Vector3();  // projectile center (incl. arc) / zone tile / beam origin
    public final Vector3 impactPos = new Vector3();  // valid during/after IMPACT or CLASH
    // geometry (mostly set at spawn)
    public int dir;                                  // +1 player-cast, -1 enemy-cast
    public int row;
    public int nearCol, farCol;                      // beam span
    public final IntArray hitTiles = new IntArray(); // strike columns
    // time (updated every tick)
    public Phase phase;                              // WINDUP / ACTIVE / RECOVERY
    public float phaseProgress;                      // 0→1 within phase
    public float elapsed;                            // since CAST
    // identity (set at spawn)
    public Element element;
    public AnimController pose;                      // the one deliberate write-crossing
}
```

Deliberately absent: the `Skill` def, textures, tints. Envelope drives
(e.g. beam intensity) are pure functions of `phase`/`phaseProgress` inside the
visual.

`AbstractSkillVisual` — base class carrying the emitter-handle bookkeeping now
in `SkillInstance` (`play(engine, effect, anchor[, drive])` collects handles;
`stopEmitters()` on END) plus one-shot helpers. `isDone()` defaults to true
after END; visuals with trailing one-shots override it.

## Gameplay-side wiring

- `Skill` gains `visual` (name string, default = id); loses `vfxTexture`,
  `zoneTexture`, `vfxAnimation`, `vfxAnimationSheet`, `vfxTint`, `shader`,
  `vfx`. Loader validates the name via `SkillVisuals.byName` at load.
- `SkillInstance` base: owns `visual` + `state`. Fires CAST on first tick
  (needs `ctx.particleEngine`). `setPhase(Phase, ctx)` helper writes state and
  fires the phase trigger once. `finish()` (→ `isResolved`) fires END.
  ```java
  public final void tick(float delta, SkillContext ctx) {
      if (!isResolved()) update(delta, ctx);   // subclass sim
      visual.update(delta);
  }
  ```
- `render(rc)` delegates to `visual.render(rc, state)`; `layer()` to the
  visual; `depth()` stays gameplay. Renderer pipeline unchanged.
- `CombatSystem`: prunes on `isFinished()`; clash/covered-tiles/hasActive use
  `isResolved()`. Clash sets each projectile's `impactPos` to the midpoint and
  fires CLASH on both visuals (element-correct reactions), replacing the
  hardcoded `Vfx.impact(Element.NONE)`.
- Lob cloud: the synthetic child `Skill` survives as gameplay data but stops
  carrying textures; its visual resolves by the id convention
  (`electro_ball_cloud` registered in the catalog). No new config field.

## Assets

Visual classes pull textures/sheets through `GameAssets` (as `Vfx` already
does), which gains cached `texture(path)` and `sheet(path, frameW, frameH,
frameDuration)` getters for arbitrary paths — centralizing disposal and
sharing. `SkillLoader`'s per-skill texture loading and `SkillLibrary`'s
disposal of them die. Skill icons stay in JSON (UI, not VFX).
`SkillShaders` stays as-is; visuals reference shader ids directly.

## Deletions (end state)

- `Skill` vfx fields + builder methods + the aura-exemption rule in `build()`
- `SkillLoader`: vfx-field parsing, `buildAnimation`, `parseColor`, texture loading
- `SkillInstance.playVfx` / `vfxHandles` / `tileAnchor` (move to `AbstractSkillVisual`)
- `SkillContext.vfxSink` and the `List<BattleVfx>` plumbing
- `ClashEffect` (no callers remain; scale-and-fade reappears as a one-shot
  helper in the visuals package)
- `CombatSystem.spawnClash`'s hardcoded impact
- All `enterAttack()`/`enterCast()` calls in shape instances
- skills.json: every `vfxTexture`/`vfxAnimation`/`vfxTint`/`shader`/`vfx`/
  `zoneTexture` block

## Migration order (each step compiles; game verified per shape)

During migration the legacy fields and render paths stay for not-yet-migrated
shapes; a skill whose id is in the `SkillVisuals` catalog uses the new path.
Legacy code is deleted in the final step.

1. **Scaffolding** — `Trigger`, `VisualState`, `SkillVisual`,
   `AbstractSkillVisual`, `SkillVisuals` registry; `GameAssets` cached getters.
   Nothing wired; game unchanged.
2. **Base wiring** — `SkillInstance` visual/state/tick/setPhase/isResolved;
   `CombatSystem` predicate split. Visual optional (null = legacy path).
3. **Projectile** — `DarkBlastVisual` (sprite + shader + darkTrail + CLASH +
   IMPACT), `FireBlastVisual`, `WindSlashVisual`. Retire the projectile's
   legacy render + clash hardcode.
4. **Lob** — `VenomBombVisual`, `ElectroBallVisual` + `electro_ball_cloud`
   (first GROUND-layer visual); synthetic-skill cleanup.
5. **Beam** — `BeamQuad` helper; `IceBeamVisual`, `ThunderVisual`,
   `FlameTorrentVisual`.
6. **Strike** — slash one-shot helper (first `isDone()` override);
   `WindStrikeVisual`, `FlameClawVisual`. Delete `ClashEffect`/`vfxSink`.
7. **Zone** — `FrostTrapVisual`, `VoidPullVisual`.
8. **Aura** — `ShieldVisual` (sprite pulse), `HealVisual`/`RegenVisual`/
   `PowerUpVisual`/`MagicUpVisual` (particles-only).
9. **Deletions + JSON cleanup** — strip legacy fields end to end; every skill
   resolves a visual by id.

Verification per step: `gradlew lwjgl3:run`, cast the migrated skills (player
+ enemy side where reachable), screenshot driver for before/after comparison.
No test infra exists; this is manual-verification territory.

## Known ceilings / parked

- Visual↔shape mismatch (a beam-reading visual on a projectile skill) fails
  visibly at runtime, not at boot. Acceptable at current catalog size.
- Status visuals: fast-follow once this lands; statuses would get the same
  registry treatment.
- Timed sub-choreography beyond triggers (e.g. "0.3s after impact") is
  expressed inside a visual's `update` or via particle emitter delays — no
  timeline DSL by design.
