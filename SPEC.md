# evoforce — Project Specification
## Overview
evoforce is a **real-time** grid-combat game for desktop, inspired by *Mega Man Battle Network*. Two characters fight on opposite halves of an 8×4 panel grid (player cols 0–3, enemy cols 4–7). Built with [libGDX](https://libgdx.com/) (Java) targeting the LWJGL3 desktop backend.
Combat is real-time. There is no turn timer; instead a **`ChargeMeter`** gates access to the staging menu, and per-skill cooldowns gate which skills are available to stage or fire.
## Tech Stack
- **Language:** Java 25
- **Framework:** libGDX 1.14.0 (uses 2D `SpriteBatch` for entities/skills and 3D `g3d.ModelBatch`+`Environment` for the cave background)
- **Post-FX:** `gdx-vfx` 0.5.4 (`VfxManager` + `BloomEffect`)
- **Desktop backend:** LWJGL3 3.4.1
- **Build tool:** Gradle (wrapper included)
- **Asset tooling:** Python 3 scripts (Pillow)
## Module Structure
```
evoforce/
├── core/                   # Platform-independent game logic
│   └── src/main/java/com/silverignis/
│       ├── Main.java
│       ├── components/                # ECS-style role components
│       │   ├── Caster.java            # composed onto entities — owns SkillDeck + SkillSlots + basicAttack + team + InputLock
│       │   ├── GridPosition.java      # composed onto entities — owns col/row + PositionSmoother + projected target
│       │   └── Team.java              # PLAYER / ENEMY (read by Caster + ProjectileInstance for team-aware logic)
│       ├── entities/
│       │   ├── Battlefield.java       # 8x4 panel grid
│       │   ├── BattleVfx.java         # short-lived VFX interface
│       │   ├── ClashEffect.java       # short-lived VFX (also reused as skill VFX)
│       │   └── Enemy.java / Player.java   # both compose Caster + GridPosition
│       ├── input/
│       │   ├── GameAction.java
│       │   ├── GamepadInputSource.java
│       │   ├── InputManager.java / InputSource.java
│       │   └── KeyboardInputSource.java
│       ├── screens/
│       │   ├── GameOverScreen.java
│       │   ├── GameScreen.java
│       │   └── MainMenuScreen.java
│       ├── skills/
│       │   ├── ChargeMeter.java
│       │   ├── Skill.java / SkillInstance.java
│       │   ├── SkillFactory.java / SkillLibrary.java
│       │   ├── SkillDeck.java                            # per-caster skill pool + cooldowns
│       │   ├── SkillCooldowns.java                       # internal to SkillDeck
│       │   ├── ShapeConfig.java / ProjectileConfig.java   # per-shape tuning data
│       │   ├── effects/Effect.java
│       │   ├── elements/Element.java
│       │   ├── instances/{Strike,Projectile,Beam,Aura,Zone}Instance.java
│       │   └── slots/{ButtonSlot,SkillSlots,SlotKey}.java   # per-caster staged loadout
│       ├── state/
│       │   ├── GameScreenState.java
│       │   ├── PlayState.java
│       │   └── SkillSelectState.java
│       ├── systems/
│       │   ├── BattleContext.java
│       │   ├── GameEnvironment.java    # 3D scene backdrop + tile→screen projection
│       │   └── CombatSystem.java
│       ├── ui/
│       │   ├── BasicAttackHud.java
│       │   ├── ChargeBarHud.java
│       │   ├── FpsHud.java
│       │   ├── LifeBarHud.java
│       │   ├── SkillSelectOverlay.java
│       │   └── SlotsHud.java
│       └── util/                       # leaf helpers (no upward deps)
│           ├── HitFlash.java            # flicker-on-hit timer (used by Player + Enemy)
│           ├── InputLock.java           # owner-based input lock used by skills
│           ├── PanelGenerator.java
│           └── PositionSmoother.java
├── lwjgl3/                 # Desktop launcher
├── assets/                 # background, music, panels/, sprites/, attacks/, effects/
└── tools/                  # Offline asset-generation scripts (Python/Pillow)
```
## Architecture Notes
### Big picture
- **Pragmatic component-style**, not a real ECS. No Ashley/Artemis. `Player` and `Enemy` are concrete classes that compose role components from `components/` (`Caster` — owns `SkillDeck` + `SkillSlots` + `basicAttack` + `team` + `InputLock`; `GridPosition` — owns `col`/`row` + `PositionSmoother` + projected-target/depth-scale) and leaf helpers from `util/` (`HitFlash`, `InputLock`, `PositionSmoother`). New behavior goes through `systems/` classes (`CombatSystem`, `BattleContext`).
- **State machine** at the `GameScreen` level: `PlayState` ↔ `SkillSelectState`, both implementing `GameScreenState`. `GameScreen` owns the *global* per-battle data (`SkillLibrary skills`, `ChargeMeter charge`); per-caster state (`SkillDeck`, `SkillSlots`, `basicAttack`, `GridPosition`) lives on the entity's components. States own *transient* per-frame logic and assets they alone need.
- **Top-level Screen flow** (peer to the in-battle state machine): `MainMenuScreen` → `GameScreen` → `GameOverScreen` → `GameScreen` (restart). `Main.setScreen` is overridden to dispose the predecessor via `Gdx.app.postRunnable` so any caller — including code running inside the predecessor's own `render` — can swap screens without freeing textures mid-frame.
- **State machine inside each `SkillInstance`**: every shape uses an inner `Phase` enum + `phaseTime` accumulator (e.g. `DASH_FORWARD → HIT → DASH_BACK` for Strike, `CHARGE → FIRE → FADE` for Beam, `APPEAR → ACTIVE → FADE` for Zone, etc.). Phase durations are tunable `private static final float`s at the top of each instance file.
- **3D-projected 2D rendering.** The battlefield is drawn inside a 3D scene (`GameEnvironment`, libGDX g3d `ModelBatch` + `Environment`) on the floor of which the panel grid lives. Each frame, entity/skill draws are placed using **tile→screen projections** the environment's camera supplies (`projectTile`, `tileDepthScale`); back-row sprites render smaller. The 2D pass is captured by a `gdx-vfx` `VfxManager` and run through `BloomEffect` before going to the framebuffer.
- **No entity animation system yet.** Entity sprites are static PNGs; a frame-based controller is deferred. *Skill VFX* can be animated (`Skill.vfxAnimation` is an `Animation<TextureRegion>` — used by `ice_beam`'s sprite-sheet beam), but `Player`/`Enemy` themselves are still single sprites tweened by `PositionSmoother`.

### Package dependency rules
The deliberate import direction, top to bottom:
```
screens, state    →    systems, entities, ui, skills, components, input
ui                →    skills, entities                (read-only, for HUD)
skills            →    entities, components, util, systems   (skills know about the world)
systems           →    entities, components            (systems coordinate entities)
entities          →    components, util                (Player/Enemy compose Caster + GridPosition + leaf helpers)
components        →    skills, util, entities          (Caster imports SkillDeck; GridPosition imports Battlefield + PositionSmoother)
util              →    (leaf — InputLock, HitFlash, PositionSmoother, PanelGenerator)
```
**`util` is leaf.** Helpers like `InputLock` and `HitFlash` live there because they're skill-agnostic — `InputLock.lock(Object)` takes `Object` so a `SkillInstance` can hold the lock without `util` depending on `skills`. Identity comparison is the contract.

**`Team` lives in `components/`, not `entities/`.** It's read by `Caster` (whose team this caster is on) and by `ProjectileInstance` (for direction). Keeping it under `components/` lets `Caster` import it without creating a `components/ → entities/` edge for an enum alone. `entities/` reads `Team` from `components/` — same direction as `Player.composes(Caster)`.

**`components/ → entities/`** exists only because `GridPosition` needs `Battlefield` for tile-center lookups. Tolerable: `entities/Battlefield.java` is itself a leaf (pure geometry + panel state, no upward deps). If we tighten the cycle ever, move `Battlefield` somewhere neutral.

**`components/` is the ECS-style role layer.** Skill-aware composable pieces live here and may freely import from `skills`. Today: `Caster` (per-caster `SkillDeck` + `SkillSlots`, composed onto Player and Enemy). Reserved for future role pieces (`CombatComponent`, etc.). Note: there is no `entities → skills` edge — entities reach skill state through the composed `Caster`.

### Per-frame loop
Every frame `GameScreen.render(delta)` runs:
1. `InputManager.update()` — refresh each child source, then OR-fold their pressed states. Edge detection (`isActionJustPressed`) lives at the manager so two sources holding the same action don't double-fire.
2. `currentState.input()` — dispatch `GameAction`s to handlers.
3. `currentState.update(delta)` — game logic (see "PlayState tick order" below).
4. `currentState.render(batch)` — clear, project, draw the world.
5. HUD pass — `ChargeBarHud` + `SlotsHud` + `BasicAttackHud` + `LifeBarHud` + `FpsHud` always draw on top of whichever state is active.

**`PlayState.update` order matters.** It is, in sequence:
`tickEntities (player.update → enemy.update → re-project tile targets + depth scale onto entities) → tickMeters (charge only — per-caster cooldowns tick inside player.update/enemy.update via the composed Caster's update → deck.update) → enemyAi → combatSystem.update → checkBattleOver (early-exit on win/lose) → tickAndCullEffects`.
Skill instances thus tick *after* entity movement (so `originCol/Row` snapshotted at fire-time is still valid for that frame). The "re-project" step pushes each entity's projected screen-space target tile and depth scale into its `GridPosition` so the smoother tweens toward the right pixel and `render()` knows the perspective scale. `checkBattleOver` reads `enemy.isDead()` (HP ≤ 0 *and* the 0.5s death fade finished) and `!player.isAlive()`; on either trigger it sets `transitionScheduled = true` and calls `Main.setScreen(new GameOverScreen(...))`. The flag short-circuits subsequent ticks so the transition only fires once.

### Render layering
`PlayState.render(batch)` runs two passes inside a `VfxManager` capture, then post-FX to screen:

**Pass 1 — 3D scene (`environment.render`):** `ModelBatch` draws the scene shell (floor, walls, ceiling, stalactites — currently themed as a cave) and the panel-grid slabs (blue on player side, red on enemy side) lit by `Environment` (ambient + directional + 4 point lights). The depth buffer is cleared afterwards so 2D never z-fights with 3D.

**Pass 2 — 2D battle (`renderWorld(batch)`), back to front:**
1. `combatSystem.renderUnder(batch)` — Zone effects only, drawn at floor level so the ground-plane art reads as terrain
2. Shadows — procedurally generated ellipse drawn at each entity's ground position
3. Entities, **Y-sorted** (higher `getVisualY()` draws first, so a player standing behind the enemy occludes correctly)
4. `combatSystem.render(batch)` — every active `SkillInstance` (beams, projectile sprites including both casters' basic attacks, auras, etc.)
5. battle VFX (`ClashEffect` and friends)

**Post-FX:** `VfxManager` ends capture, applies `BloomEffect` (base 1.0, bloom 1.2, threshold 0.25), and blits to screen.

**HUD pass** (`GameScreen.render`) draws `ChargeBarHud`, `SlotsHud`, `BasicAttackHud`, `LifeBarHud`, and `FpsHud` on top of everything — outside the VFX capture, so HUD doesn't bloom.

`ZoneInstance.isRenderUnder()` is the single hook that splits combat-system draws across layers 1 and 5.

### Skill execution data flow
```
PlayState.handleSlotFire
   └─ tryFireSlot(key)
        ├─ player.isInputLocked() ?  → bail
        ├─ slot.pop()                → Skill (pure data)
        ├─ deck.onUsed(skill)        → start its cooldown timer
        ├─ SkillFactory.create(skill, caster, gridPosition)   → SkillInstance (behavior)
        └─ combatSystem.spawn(instance)                       → registered for ticking
```
- `Skill` is reusable, immutable data. Many `SkillInstance`s can run from the same `Skill`.
- `SkillInstance` owns its own state machine; subclasses override `update(delta, ctx)` and optionally `render(batch, ctx)`. The base class wires up `acquireInputLock()` / `releaseInputLock()` / `finish()` boilerplate.
- A skill instance can spawn child instances (e.g. `ProjectileInstance` LOB landing spawns a `ZoneInstance` "cloud") via `ctx.combatSystem.spawn(...)`. `BattleContext.combatSystem` is set after construction precisely so this back-edge exists.
- One-shot visuals go into `ctx.vfx` (drawn by `PlayState` after entities); persistent visuals stay inside the instance and draw via `CombatSystem.render`.
- `CombatSystem.update` iterates over a snapshot index so an instance that spawns another mid-tick doesn't get double-ticked the same frame.

### Input pipeline
- `InputSource` is the unified pollable interface (`isActionPressed`, `isActionJustPressed`, `update`).
- `KeyboardInputSource` keeps no internal state: it forwards both queries to `Gdx.input.isKey{Pressed,JustPressed}` and lets libGDX manage edges.
- `GamepadInputSource` *does* keep its own current/previous maps because gdx-controllers is event-driven (`buttonDown`/`buttonUp`/`axisMoved` callbacks). Trigger axes are thresholded (`> 0.5`) into booleans because L2/R2 arrive as analog values.
- `InputManager` composes any number of sources, ORs their pressed state per `GameAction`, and runs *its own* edge detection on the OR-folded result. This is the layer that prevents double-fires when both keyboard and pad are pushing the same action.

### Resource ownership & disposal
Textures and GPU resources are owned by the closest scope that needs them and disposed on the way back up:
- `Battlefield` owns no GPU resources — it's pure geometry + panel state; the floor is drawn by `GameEnvironment`'s 3D panel meshes.
- `SkillLibrary` owns each skill's icon **and** its VFX texture (plus the sprite sheet backing any `Skill.vfxAnimation`); all disposed in `SkillLibrary.dispose()`.
- `GameEnvironment` owns the wall/floor textures, `ModelBatch`, and every `Model` it builds.
- `PlayState` owns sprite sheets, the clash texture, the procedural shadow texture, the `VfxManager`, the `BloomEffect`, and the `GameEnvironment` instance. Both casters' basic-attack icons and VFX textures are owned by `SkillLibrary` (loaded from the `wind_slash` JSON entry).
- HUDs own their 1×1 pixel pixmaps (used for tinted rectangles).
- `GameScreen.dispose()` cascades into `playState`, `skillSelectState`, `chargeHud`, `slotsHud`, `basicAttackHud`, `lifeBarHud`, `skills`.
- **Cross-screen disposal is centralized in `Main.setScreen`.** The override captures the outgoing screen, calls `super.setScreen(...)` (which fires `hide` on the old and `show`/`resize` on the new), then schedules `old.dispose()` via `Gdx.app.postRunnable`. Deferring to the next frame avoids the mid-render hazard where a screen's own `render()` call stack is still unwinding when `setScreen` is invoked. Individual screens therefore do **not** self-dispose after `setScreen`.

**Don't add `new Texture(...)` calls inside per-frame paths.** Construct in `*.create()`/state constructors, dispose alongside. The shadow texture is the canonical example — built once procedurally in `PlayState.buildShadowTexture()` and reused every frame.
## Core Classes
### `Main` (extends `Game`)
Application root. Owns shared rendering resources (`SpriteBatch batch`, `BitmapFont font`, `FitViewport viewport` — 16×9). `create()` pushes `MainMenuScreen`. Overrides `setScreen(Screen)` to dispose the predecessor via `Gdx.app.postRunnable` — see "Resource ownership & disposal" above.
### Screens
- **`MainMenuScreen`** — splash; switches to `GameScreen` on touch.
- **`GameScreen`** — owns the global per-battle objects (`SkillLibrary skills`, `ChargeMeter charge`) and HUDs (`ChargeBarHud`, `SlotsHud`, `BasicAttackHud`, `LifeBarHud`, `FpsHud`). Per-caster state (`SkillDeck`, `SkillSlots`, the basic-attack holder) lives on the `Caster`, not here. Constructor seeds `player.getDeck()` with every library skill *except* `wind_slash` — that one is assigned to `player.getCaster().setBasicAttack(...)` and fired via the dedicated `ATTACK_BASIC` button, so it deliberately stays out of the staging hand. Delegates per-frame `input/update/render` to whichever `GameScreenState` is active; the HUD pass draws on top of the state's rendering.
- **`GameOverScreen`** — post-battle "YOU WON" / "YOU LOST" overlay shown on a solid black backdrop. Constructed with a `Result { WON, LOST }`. Owns its own `InputManager` (default keyboard + gamepad). Restarts on `SKILL_SELECT_CONFIRM` (Enter or A) by calling `Main.setScreen(new GameScreen(...))`; the override disposes the spent `GameOverScreen` next frame.
### States
- **`PlayState`** — real-time combat. Reads movement (WASD), basic attack (J), slot fire (1/2/3), and the `Q+E` trigger combo to open the staging menu (gated on `charge.isFull()`). Owns `BattleContext` and `CombatSystem`. Slot pops go through `player.getSlots()`; `onUsed` goes through `player.getDeck()`; both `handleAttack` (player basic attack) and `enemyAi` (enemy basic attack) fire `SkillFactory.create(skill, caster.getCaster(), caster.getGridPosition())` through the same `CombatSystem.spawn` path. Per-caster cooldowns tick inside `<entity>.update → caster.update → deck.update` (`tickMeters` only handles the global `ChargeMeter` now). Each frame after `combatSystem.update` it calls `checkBattleOver`: on `enemy.isDead()` (post-fade) it switches to `GameOverScreen(WON)`; on `!player.isAlive()` it switches to `GameOverScreen(LOST)`. A `transitionScheduled` flag prevents repeated transitions during the one-frame gap before disposal fires.
- **`SkillSelectState`** — staging menu. `enter()` snapshots `player.getSlots()`, draws a hand of 6 via `player.getDeck().drawHand(HAND_SIZE, player.getSlots())` (filtered by deck cooldowns + slots already loaded). Cursor navigation is `MOVE_LEFT/RIGHT`; `SKILL_X/Y/B` assign the highlighted card to that slot; `Enter` confirms (drains charge); `Esc`/`Back` cancels (slots restored from snapshot, charge preserved). Renders the world frozen behind the overlay.
### Entities
- **`Battlefield`** — `COLS=8 × ROWS=4` grid of `PanelType` enum values. Cols 0–3 are player territory, cols 4–7 enemy. Panel types: `NORMAL_BLUE`, `NORMAL_RED`, `CRACKED`, `BROKEN`, `ICE`, `LAVA`, `GRASS`, `POISON`. `PanelGenerator` has two factories: `generatePanels()` (default flat blue/red split, used today) and `generateMixedPanels()` (sprinkles hazard tiles — useful for testing once panel effects are wired).
- **`Player`** — One cell `(col,row)`, hp 100 (`maxHp` retained for the life bar's ratio math; exposed via `getMaxHp()`), half-grid clamped on `moveLeft/Right` (`col ∈ [0, COLS/2 - 1]`). Composes a `Caster(Team.PLAYER)` (owns `SkillDeck` + `SkillSlots` + the swappable `basicAttack` skill + `InputLock`) and a `GridPosition` (owns `col`/`row`/`PositionSmoother`/projected-target/depth-scale). `getCol/getRow/getVisualX/Y/getDepthScale` delegate to `GridPosition`; `getTeam/getInputLock/getDeck/getSlots/getBasicAttack` delegate to `Caster`. `HitFlash` provides the 300ms post-hit invisible-frame flicker. `update(delta)` ticks `caster.update(delta)` + `gridPosition.update(delta)` + `hitFlash.tick(delta)`. `forceSetTile(col,row)` writes via `gridPosition.setTile` (unclamped) so `StrikeInstance` can drive the body into enemy territory mid-skill. `takeDamage(int)` reduces `hp` and calls `hitFlash.flash()`. `applyFreeze(float)` is a no-op stub — player status state is reserved for a future component. **No bespoke basic-attack code** — pressing `ATTACK_BASIC` fires the `Skill` held in `getBasicAttack()` through the standard `SkillFactory` + `CombatSystem` pipeline (the same one slot fires use), gated by `getDeck()`'s cooldown map.
- **`Enemy`** — Mirror, constrained to cols 4–7 (`col ∈ [COLS/2, COLS - 1]`), hp 100. Composes a `Caster(Team.ENEMY)` (its deck is empty today — only the `basicAttack` slot is set to `wind_slash`) and a `GridPosition`. Inherits no class — same composition pattern as Player. Placeholder AI: `stepRandomly()` picks a random direction every 0.5–1.5s. `PlayState.enemyAi()` fires the held `basicAttack` through the unified pipeline every time the deck cooldown is up. **Status & death are wired**: `applyFreeze(duration)` halts the cooldown gate and tints the sprite blue (`FREEZE_TINT`); `isFrozen()` is the gate read by `PlayState.enemyAi`. When `hp <= 0` the enemy enters a 0.5s death fade (`isDying()` → sprite + shadow alpha lerp); `isDead()` reports when it's safe to remove. `render(batch, font)` also draws a small HP number centered just under the sprite (alpha fades with the death animation).
- **`Team`** (in `components/`) — `PLAYER` / `ENEMY` enum read by `Caster` (whose side the caster fights for) and by `ProjectileInstance` (to flip direction and pick the opposing target).
- **`BattleVfx`** (interface) / **`ClashEffect`** — `BattleVfx` is `update + render + isAlive`. `ClashEffect` scales 0.6→1.6 and fades over 0.25s; `PlayState` culls when `!isAlive()`.
### Skills
- **`Skill`** — pure, immutable data: `id`, `displayName`, `description`, `icon`, `Shape` (enum: `PROJECTILE/BEAM/STRIKE/AURA/ZONE`), `Element`, `List<Effect>` (unmodifiable), `cooldown`, **required** `vfxTexture`, optional `vfxAnimation` (`Animation<TextureRegion>` — when present, renderers prefer it over the static texture; this is how `ice_beam` animates) backed by an owned `vfxAnimationSheet` `Texture` so disposal is symmetric, optional `ShapeConfig`. The same `Skill` can spawn many simultaneous `SkillInstance`s. Constructors are private — build with `Skill.builder()....build()`. The builder validates required fields and throws `IllegalStateException` naming the offending field on `build()`.
- **`ShapeConfig` / `ProjectileConfig`** — marker interface + per-shape tuning record. `ProjectileConfig` is the only concrete one today: `straight(speed)` for laser-style movement, `lob(targetRange, arcHeight)` for arcing tosses that land after `LOB_FLIGHT_TIME`. Add a new `ShapeConfig` impl when a shape grows tunable parameters.
- **`SkillInstance`** — abstract base for runtime executions. Tracks `caster: Caster` and `pos: GridPosition` (the firing entity's two components), snapshotted origin tile (`originCol/originRow` captured from `pos` at construction), `finished/lockTaken` flags. Methods: `acquireInputLock()` (via `caster.getInputLock()`), `releaseInputLock()`, `finish()`, abstract `update(delta, BattleContext)`, default no-op `render(batch, ctx)`. Provides two `applyEffectsTo` overloads — `(Enemy)` (existing) and `(Player)` (new; FREEZE no-ops on Player until a status component lands).
- **`SkillFactory.create(Skill, Caster, GridPosition)`** — single static switch on `Shape` returning the right `SkillInstance` subclass. Adding a new shape means adding an enum entry, a switch case, and a new `SkillInstance` subclass. Pure-component params — works for either Player or Enemy as the caster.
- **`SkillLibrary`** — global catalogue of every `Skill` the game knows about, and the owner of their GPU resources (icons, VFX textures, VFX animation sheets — all disposed by `SkillLibrary.dispose()`). `defaults()` loads from `assets/skills/skills.json` via `SkillLoader.load(...)`. The starter pool today is eight skills:
    - `wind_strike` — STRIKE, 15 dmg, 2.0s cd
    - `fire_blast` — PROJECTILE/straight (8 u/s), 20 dmg, 3.0s cd
    - `venom_bomb` — PROJECTILE/lob (range 2, arc 2.0), 12 dmg + spawns a `ZoneInstance` "cloud" on landing, 3.5s cd
    - `ice_beam` — BEAM, 25 dmg **+ 2.0s FREEZE @ 100% chance**, 4.0s cd; uses an `Animation<TextureRegion>` sliced from `skills/animations/icebeam_spritesheet.png` (0.1s frame time)
    - `frost_trap` — ZONE/Ice, 5 dmg + 2.0s FREEZE @ 100%, 3.5s cd
    - `heal` — AURA, 5.0s cd (per-tick effect TODO)
    - `shield` — AURA, 4.0s cd (per-tick effect TODO)
    - `wind_slash` — PROJECTILE/straight (12 u/s), 5 dmg, 0.5s cd, element NONE. Held in `Caster.basicAttack` by default; **not** added to the player's deck, so it never appears in `drawHand`.
  The library does **not** hand-draw — that's the deck's job. `GameScreen` seeds the player's deck with every library entry except `wind_slash` at battle start.
- **`SkillDeck`** — per-caster standalone container of `Skill` references plus a private `SkillCooldowns`. Owned by a `Caster` (see Components). Methods:
    - `add(Skill)` / `remove(Skill)` / `contains(Skill)` / `all()` — membership; backed by a `LinkedHashSet<Skill>` so iteration order is stable for future inventory UIs (`drawHand` shuffles anyway).
    - `update(delta)` / `onUsed(Skill)` / `isOnCooldown(Skill)` / `remainingFor(Skill)` / `clearCooldowns()` — thin facade over the private `SkillCooldowns`.
    - `available()` — deck skills not currently on cooldown.
    - `drawHand(n, SkillSlots slots)` — eligible (off-cooldown + not in `slots`), shuffled, sliced to `n`. The deck has **no** back-reference to its caster; callers pass `SkillSlots` explicitly.
- **`SkillLoader`** — parses `skills.json` (top-level array) via libGDX's `JsonReader`/`JsonValue`. Each entry maps directly onto `Skill.builder()` setters; `effects` is a polymorphic list discriminated by `type` (matching `Effect.Type`); `shapeConfig.movementType` (`STRAIGHT`/`LOB`) dispatches to the existing `ProjectileConfig.straight(...)`/`.lob(...)` factories; `vfxAnimation` carries `spritesheet`, `frameWidth`, `frameHeight`, `frameDuration`. Missing/malformed fields throw `IllegalStateException` naming the skill id and field path — no silent skipping.
- **`SkillCooldowns`** — `Map<String, Float>` remaining time. `update`, `onUsed`, `isOnCooldown`, `remainingFor`, `clear`. Lives inside a `SkillDeck` (one per caster); not exposed directly anywhere else. Ticked via `Player.update → caster.update → deck.update`.
- **`ChargeMeter`** — fills over time (max=1, fillRate=0.20/s ≈ 5s). `isFull()` gates the staging menu; `consume()` drains on confirm; `add(amount)` for future "charge on kill / on hit" rewards.
- **`StrikeInstance`** — Phases: `DASH_FORWARD` (0.10s) → `HIT` (0.20s) → `DASH_BACK` (0.10s) → `DONE`. Acquires the caster's `InputLock`, snaps to `originCol+1` via `pos.setTile(...)` (unclamped), spawns a `ClashEffect` on the target tile (`originCol+2`), applies `DAMAGE`-typed effects to `ctx.enemy` if it occupies the target tile, then snaps back. Targets `ctx.enemy` directly — assumes player firing; enemy firing a Strike would aim at itself today. Status-effect TODOs (`POISON/BURN/STUN/FREEZE`) are flagged inline.
- **`ProjectileInstance`** — Two movement modes via `ProjectileConfig`. **Team-aware**: `dir = caster.getTeam() == PLAYER ? +1 : -1`, applied to spawn-offset, velocity sign, grid-exit edge, sprite horizontal flip, and target lookup.
    - `STRAIGHT`: spawns at the caster's visual position and travels `dir * config.speed`; finishes when it crosses the appropriate grid edge (right for player, left for enemy) or overlaps the opposing entity's tile center on the same row. `checkHitStraight` reads `ctx.enemy` when `caster.getTeam() == PLAYER` and `ctx.player` otherwise.
    - `LOB`: parabolic arc from caster to `originCol + targetRange * dir` over `LOB_FLIGHT_TIME` (0.50s); on landing applies damage if the enemy is on the landing tile, then spawns a child `ZoneInstance` (the "cloud") via `ctx.combatSystem.spawn(...)` for lingering effect. LOB-from-enemy is latent (no enemy fires LOB skills today).
  Does **not** acquire `InputLock` — the caster keeps playing while their projectile flies. Sprite size is resolved lazily from `ctx.battlefield` on the first update tick (no dependence on the caster's own sprite).
- **`BeamInstance`** — Phases: `CHARGE` (0.20s) → `FIRE` (0.70s) → `FADE` (0.25s) → `DONE`. Acquires `InputLock` for the windup; releases at the start of `FADE`. Hits everything on the caster's row from `originCol+1` rightward exactly once on entering `FIRE`. Render: if `Skill.vfxAnimation` is present (e.g. `ice_beam`'s sprite-sheet) the beam draws the current `Animation` frame; otherwise it stretches the static `vfxTexture`. During `CHARGE` the beam extends out from the caster (`w = fullW * (phaseTime/CHARGE)`); during `FADE` alpha lerps to zero.
- **`AuraInstance`** — Phases: `EXPAND` (0.20s) → `ACTIVE` (3.00s) → `FADE` (0.20s) → `DONE`. Renders a scaled, pulsing sprite on the caster's tile. `applyTick(ctx)` runs every `TICK_INTERVAL` (0.50s) but is a TODO until self-targeted effects exist.
- **`ZoneInstance`** — Phases: `APPEAR` (0.15s) → `ACTIVE` (1.00s) → `FADE` (0.25s) → `DONE`. Pinned to a target tile (defaults to `caster +1, sameRow`; can be overridden — that's how `ProjectileInstance` plants a cloud on its landing tile). Damages whatever opposing entity is standing on the tile every `TICK_INTERVAL` (0.33s). Returns `true` from `isRenderUnder()`, so it draws under entities (terrain feel).
- **`slots/`** — `SlotKey` (`X, Y, B`), `ButtonSlot` (FIFO `ArrayDeque<Skill>`, `CAPACITY=2`), `SkillSlots` (`EnumMap<SlotKey, ButtonSlot>` + `contains(Skill)`). A `SkillSlots` instance lives on each `Caster` (accessed via `caster.getSlots()`) — not on `GameScreen`. Slots persist across `PlayState` ↔ `SkillSelectState` transitions; cancel restores from a snapshot taken on `enter()`.
- **`effects/Effect`** — `Type` enum (`DAMAGE, POISON, BURN, STUN, FREEZE`), with `value`, `duration`, `chance` fields. `SkillInstance.applyEffectsTo(...)` has two overloads — `(Enemy)` consumes `DAMAGE` and `FREEZE` (rolls `chance`, calls `applyFreeze`); `(Player)` consumes `DAMAGE` but the player's `applyFreeze` is a no-op stub until a player status component lands. `POISON`, `BURN`, and `STUN` are still reserved.
- **`elements/Element`** — `NONE, FIRE, POISON, ICE, LIGHTNING`. Currently cosmetic — no element-vs-element resistance/weakness logic yet.
### Systems
- **`BattleContext`** — bag of references (battlefield, player, enemy, `List<BattleVfx> vfx`, `GameEnvironment environment`, *and* a back-reference to its owning `combatSystem` set immediately after construction). Constructed once in `PlayState`. Projection helpers `projectedTileWorld(col,row)` (tile center in 2D viewport space) and `tileDepthScale(row)` (1.0 near → ~0.78 far) are how skill instances place their VFX correctly on the 3D-projected floor. `buildCache()` (called from `PlayState.resize`) bakes a `COLS×ROWS` lookup so every per-frame skill draw is a constant-time array hit, not a `cam3D.project()` call. Pass-around context (not a service locator) so skills can be unit-tested with fakes.
- **`CombatSystem`** — owns `List<SkillInstance> active`.
    - `spawn(instance)` adds to the active list (callable from anywhere with a `BattleContext`, including from inside another `SkillInstance.update`).
    - `update(delta)` ticks every active instance over a snapshot index — instances spawned mid-tick join the list but won't be ticked until next frame.
    - `renderUnder(batch)` draws zone instances flagged `isRenderUnder()`; `render(batch)` draws everything else. Two-pass rendering is what lets zone tiles sit visually beneath player/enemy sprites.
    - `hasActive()` for future menu/pause logic.
- **`GameEnvironment`** — the 3D scene that hosts the battlefield (currently dressed as a cave; the class itself is theme-agnostic). Owns a `PerspectiveCamera` (45° FOV, positioned high and tilted ~40° below horizontal), an `Environment` with ambient + directional + four point lights tinted to match the crystal palette, and a `ModelBatch` driving `ModelInstance`s built once in `buildGeometry()` (floor slab, back wall, ceiling, side walls, eight stalactites, and the COLS×ROWS panel grid — blue diffuse + blue-specular on the player side, red on the enemy side, so the point lights cast tinted glows on each tile). Exposes two helpers consumed by `BattleContext`: `projectTile(col,row)` maps a tile center on the 3D floor into 2D viewport world coordinates so sprites can be drawn standing on it, and `tileDepthScale(row)` returns a linear scale `1.0 → 1.0 - DEPTH_SCALE_FAR (0.22)` from near to far row. `dispose()` releases all built `Model`s plus the wall/floor textures and the `ModelBatch`.
### Components
The `components/` package is for ECS-style role components — entities compose them to opt into behaviors. Unlike leaf helpers in `util/`, components in this package may freely depend on `skills`.
- **`Caster`** — role component for entities that can stage and cast skills. Owns:
    - a final `Team team` (set in ctor; read by team-aware skill instances and exposed via `getTeam()`),
    - a final `SkillDeck deck` (skills the caster has access to + their cooldown timers),
    - a final `SkillSlots slots` (the X/Y/B staged loadout),
    - a final `InputLock inputLock` (held by skill instances during dash/cast — gates both entity movement and further skill firing),
    - a mutable `Skill basicAttack` (the skill fired by the dedicated `ATTACK_BASIC` button — swappable at runtime via `setBasicAttack(Skill)`).
  Exposes `getTeam()` / `getDeck()` / `getSlots()` / `getInputLock()` / `getBasicAttack()` / `setBasicAttack(Skill)` and an `update(delta)` that ticks `deck.update(delta)`. Composed onto `Player` and `Enemy`; the entity's own `update` must call `caster.update(delta)` so cooldowns tick. The entity also delegates these getters directly so call sites stay clean (`player.getBasicAttack()` rather than `player.getCaster().getBasicAttack()`).
- **`GridPosition`** — position component for entities living on the panel grid. Owns `col`, `row`, a `PositionSmoother` (world-space tween between tile centers), `projectedTargetX/Y` (the screen-projected target `PlayState.tickEntities` pushes in each frame), and `depthScale` (perspective scale for the row). `setTile(col, row)` is unclamped — the owning entity is responsible for movement rules (half-grid clamp for Player, enemy-territory clamp for Enemy). `update(delta)` smooths toward the projected target (or tile center, if no target yet). Composed onto `Player` and `Enemy`; the entity's own `update` calls `gridPosition.update(delta)`. `forceSetTile` on entities is just a thin wrapper around `gridPosition.setTile(...)` used by `StrikeInstance` to drive the player across territory.
- **`Team`** — `PLAYER` / `ENEMY` enum. Lives here (not in `entities/`) so `Caster` can hold one without `components/` having to import `entities/`.

### Util (leaf helpers)
- **`InputLock`** — owner-based lock. `lock(owner)` refuses if a different owner holds it; `unlock(owner)` is a no-op if you're not the holder. Owner is `Object` (not `SkillInstance`) so `util` stays a leaf package and skill instances can hold the lock without `util` depending on `skills`. Identity comparison is all that matters.
- **`HitFlash`** — post-hit flicker timer (0.3s total, 0.05s flicker interval). `flash()` arms; `tick(delta)` decays; `isHidden()` returns true on alternating intervals while armed. `Player.render` and `Enemy.render` skip the draw when `isHidden()` is true, producing the invisible-frame strobe.
- **`PositionSmoother`** — exponential easing toward a target `(x, y)`. Used by `Player` and `Enemy` to tween between tile centers.
- **`PanelGenerator`** — static factories that produce the `PanelType[][]` grid handed to `Battlefield`. Two presets: `generatePanels()` (flat blue/red split, used today) and `generateMixedPanels()` (sprinkles hazard tiles).
### UI
- **`ChargeBarHud`** — small charge meter sitting just above the `SlotsHud` panels in the bottom-left, width-matched to the X/Y/B slot row so the two HUDs read as one stack. Batch-aware (`wasDrawing` check).
- **`SlotsHud`** — bottom-left X/Y/B columns with FIFO icons (top of column = front of queue), placeholder for empty cells.
- **`BasicAttackHud`** — bottom-left icon one slot-gap to the right of the X/Y/B column, showing the skill held in `player.getBasicAttack()`. Renders a translucent black "cooldown veil" over the top portion of the icon when on cooldown — the veil shrinks downward as `deck.remainingFor(skill)` ticks toward 0, the standard MOBA-HUD cooldown sweep. Owns a 1×1 white pixel disposed by `GameScreen`.
- **`LifeBarHud`** — horizontal player HP bar anchored to the top-left of the viewport. Fill ratio is `player.getHp() / player.getMaxHp()`; tint shifts green → yellow → red as HP drops. Owns a 1×1 white pixel disposed by `GameScreen`.
- **`FpsHud`** — top-right frame-rate readout. Samples `Gdx.graphics.getFramesPerSecond()` every 0.25s (so the text doesn't flicker each frame).
- **`SkillSelectOverlay`** — card hand renderer used by `SkillSelectState`. Hand size is currently 6; horizontal layout was originally tuned for 4, so visual fit may need a pass.
## Viewport & Coordinates
- World size: **16 × 9** world units (matches a 16:9 display).
- Battlefield occupies roughly `10 × 4` world units, placed at `(3, 1)`.
- Each panel cell is `10/8 = 1.25` units wide × `4/4 = 1.0` units tall (logical; rendered height is compressed by `Battlefield.RENDER_HEIGHT_SCALE = 0.60` for the 2.5D floor illusion).
## Input Map
Keyboard:
| Key | `GameAction` | Meaning |
|---|---|---|
| W / A / S / D | `MOVE_*` | Grid movement |
| J | `ATTACK_BASIC` | Fire the player's basic-attack skill (`player.getBasicAttack()`) |
| Q | `TRIGGER_LEFT` | Hold with E to open staging menu (when charge full) |
| E | `TRIGGER_RIGHT` | Hold with Q to open staging menu (when charge full) |
| 1 / 2 / 3 | `SKILL_X` / `SKILL_Y` / `SKILL_B` | Fire slot in `PlayState`; assign in `SkillSelectState` |
| Enter | `SKILL_SELECT_CONFIRM` | Confirm staging (consumes charge) |
| Esc | `SKILL_SELECT_CANCEL` | Cancel staging (restores snapshot) |
Gamepad (XInput-style mapping via gdx-controllers):
| Button | `GameAction` | Meaning |
|---|---|---|
| D-pad / Left stick | `MOVE_*` | Grid movement |
| A | `ATTACK_BASIC` + `SKILL_SELECT_CONFIRM` | Shared: attack in play, confirm in menu |
| X / Y / B | `SKILL_X` / `SKILL_Y` / `SKILL_B` | Fire slot in `PlayState`; assign in `SkillSelectState` |
| L2 / R2 (triggers) | `TRIGGER_LEFT` / `TRIGGER_RIGHT` | Hold both to open staging menu |
| Back / Select | `SKILL_SELECT_CANCEL` | Cancel staging |
The `SKILL_X/Y/B` actions are dual-purpose by state: in `PlayState` they fire the front of the slot, in `SkillSelectState` they assign the highlighted hand card. Gamepad cancel is on Back rather than B so pressing B inside the menu doesn't double-fire (assign + cancel). `SKILL_SELECT_CONFIRM` (Enter / A) is reused by `GameOverScreen` for the post-battle restart so no new bindings were needed.
## Skill Flow (MVP)
1. Wait ~5s for the `ChargeMeter` to fill.
2. Hold Q+E (or L2+R2) → `GameScreen` flips to `SkillSelectState`, world freezes, hand of 6 drawn.
3. ←/→ to move the cursor; 1/2/3 (or X/Y/B) to assign the highlighted card to that slot. Slots that were already loaded persist; the menu is "top up," not "rebuild."
4. Enter to confirm (drains charge, returns to `PlayState`) or Esc/Back to cancel (slots restored from snapshot, charge kept).
5. In play, 1/2/3 (or X/Y/B) fires the front of that slot via `SkillFactory.create()` → `CombatSystem.spawn()`. The skill's cooldown starts.

The basic attack lives outside this flow: pressing **J** (or **A**) at any time fires `player.getBasicAttack()` through the same `SkillFactory` → `CombatSystem` pipeline, gated by `getDeck().isOnCooldown(skill)`. No charge, no staging — that's why the basic-attack skill is intentionally kept out of the deck (so it never appears in the staging hand).

## Battle End Flow
1. `PlayState.checkBattleOver` (called each frame after `combatSystem.update`) reads `enemy.isDead()` and `!player.isAlive()`.
2. On win, the existing 0.5s enemy death fade plays first (`isDying`), then `isDead` flips true and `Main.setScreen(new GameOverScreen(WON))` fires.
3. On loss, transition is immediate — player has no death fade yet (see Known Gaps).
4. `Main.setScreen` swaps screens and queues `GameScreen.dispose()` for the next frame via `postRunnable`.
5. `GameOverScreen` shows "YOU WON" / "YOU LOST" + the restart hint; pressing **Enter** or **A** calls `Main.setScreen(new GameScreen(...))`, which in turn disposes the spent `GameOverScreen` — yielding a fully fresh battle every restart.
## Design Rationale
- **Charge gates the menu, cooldowns gate the hand.** Two coexisting timers with different jobs: `ChargeMeter` controls when you can stage, per-skill cooldowns control which skills are *available* to stage or fire.
- **Slots persist across menu opens** (Battle Network "custom screen" feel).
- **Snapshot/restore on cancel** rather than journaling individual assigns. Simpler and player-indistinguishable.
- **`InputLock` owner is `Object`** so `util/` stays a leaf package — a `SkillInstance` can hold the lock without `util` depending on `skills`. Identity comparison is all that matters.
- **Per-caster state lives on `Caster`, not `GameScreen`.** `SkillDeck` (which owns its own `SkillCooldowns`) and `SkillSlots` are properties of *this* casting entity. Cooldowns are the player's, not the battle's; the same applies to the staged loadout. `GameScreen` keeps only what's truly battle-global: the skill catalogue, the charge meter, and the HUDs.
- **`SkillDeck` has no caster back-reference.** Membership + cooldowns are intrinsic to the deck, but anything that needs to filter by slots (like `drawHand`) takes them as a parameter — keeps `SkillDeck` a standalone data structure and avoids the `Caster ↔ SkillDeck` loop.
- **`forceSetTile` bypasses the half-grid clamp** because a Strike's HIT phase puts the player in enemy territory. The clamp on `moveLeft/Right` is correct for normal movement and wrong for skills. It's a thin wrapper around `gridPosition.setTile(...)` (which is unclamped by design).
- **VFX reuses `ClashEffect`** because it already does scale+fade over a short lifetime — exactly the slash flourish we want. Per-skill VFX textures live on `Skill.vfxTexture` and are required non-null — every skill must supply one.
- **Hit resolution lives inside each `SkillInstance`** (synchronous, fixed-timing). There's no separate collision system — every projectile, beam, strike, and zone does its own tile-center overlap test inside its `SkillInstance` subclass. `ProjectileInstance` is team-aware and picks the right opposing entity (`ctx.enemy` for player-fired, `ctx.player` for enemy-fired).
- **`SkillFactory` takes pure components, not entities.** `SkillFactory.create(Skill, Caster, GridPosition)` — works for any entity that composes those two components. Avoids coupling the skill pipeline to a particular entity class. Both `PlayState.handleAttack` (player) and `PlayState.enemyAi` (enemy) call it the same way; only the source `caster.getCaster()` / `caster.getGridPosition()` differs.
- **Child instances spawn via `ctx.combatSystem.spawn(...)`.** A `ProjectileInstance` LOB landing plants a `ZoneInstance` cloud this way. Avoid coupling instances directly — always go through the combat system so spawn order, layering, and reaping stay consistent.
- **Screen disposal centralized in `Main.setScreen`** rather than per-screen `dispose()` calls after a transition. The previous self-dispose idiom (still visible in old libGDX tutorials) was unsafe whenever a heavy screen swapped itself out mid-render — it would free textures still in use further up the call stack. Deferring the dispose via `Gdx.app.postRunnable` makes every call site `Main.setScreen(...)`-and-done.
## Asset Pipeline
| Script | Output |
|---|---|
| `generate_battlefield.py` | `assets/battlefield.png` (full grid texture) |
| `generate_panels_pixellab.py` | Individual panel PNGs under `assets/panels/` |
| `generate_sprites_pixellab.py` | Sprite sheets under `assets/sprites/` |
| `generate_attacks_pixellab.py` | Basic-attack sprites under `assets/attacks/` |
| `generate_dungeon_pixellab.py` | Cave wall/floor textures (`assets/cave_*.png`) |
| `generate_skill_vfx.py` | Skill VFX sprites under `assets/effects/` and `assets/skills/animations/` |
| `generate_projectile_vfx.py` | Projectile VFX sprites under `assets/effects/` |
| `generate_clash.py` | `assets/effects/clash.png` |
An `assets.txt` manifest is auto-generated at build time by the `generateAssetList` Gradle task.
## Build & Run
```bash
# Run on desktop
./gradlew lwjgl3:run
# Build runnable JAR
./gradlew lwjgl3:jar
# Output: lwjgl3/build/libs/evoforce-1.0.0.jar
```
## Known Gaps / Future Work
- **No entity animation system.** `Skill.vfxAnimation` covers animated VFX (used by `ice_beam`), but `Player`/`Enemy` themselves are still single static sprites. Pattern when ready: `AnimController` component holding `Map<State, Animation<TextureRegion>>` + current state + elapsed time, ticked by an `AnimSystem`. `Player.render()` would read the current frame; `StrikeInstance` would set `ATTACKING` on entering `HIT`, `NEUTRAL` on entering `DASH_BACK`.
- **`AURA` shape's per-tick effect is stubbed.** `AuraInstance.applyTick` is a TODO; `heal` and `shield` are valid skill data with a working visual envelope but no gameplay effect yet. Self-targeted effects need to land before this can be filled in.
- **Status effects partially wired.** `applyEffectsTo(Enemy)` consumes `DAMAGE` and `FREEZE` (the latter halts enemy movement/attack and tints the sprite). `applyEffectsTo(Player)` consumes `DAMAGE` but `Player.applyFreeze` is a no-op stub — the player has no freeze state yet. `POISON`, `BURN`, `STUN` are reserved for both targets. Add a `StatusComponent` on entities (or fold the timers into `Caster`) and route through the same effects loop.
- **Projectile-vs-projectile cancels are gone.** The old `CollisionResolver` Pass A canceled opposing projectiles on overlap and spawned a `ClashEffect`. With both sides now firing through independent `ProjectileInstance`s and the legacy `Projectile` class deleted, projectiles pass through each other. Re-add as pairwise iteration over active `ProjectileInstance`s inside `CombatSystem` (filter by `getCaster().getTeam()` mismatch, overlap their sprite bounds, spawn a `ClashEffect` and `finish()` both).
- **Strike/Beam/Aura/Zone assume player firing.** They take a `Caster` now but their internals target `ctx.enemy` and use player-relative directions. If enemies ever fire those shapes, each needs the same team-aware target lookup `ProjectileInstance` got.
- **No I-frames during Strike.** Player can be hit while dashing. Simplest fix: an invulnerable flag that each `ProjectileInstance.checkHitStraight` (and friends) check before applying effects.
- **Enemy doesn't use staged skills yet.** It fires `wind_slash` via `Caster.basicAttack` through the unified pipeline, but its `SkillDeck` and `SkillSlots` are empty and no AI logic stages anything. When enemies need to cast staged skills: populate the deck (`enemy.getDeck().add(...)`), and write an AI that picks a skill, runs the same `deck.onUsed` + `SkillFactory.create(skill, enemy.getCaster(), enemy.getGridPosition())` + `combatSystem.spawn` sequence `PlayState.enemyAi` already does for basic attack.
- **Enemy AI is a placeholder** — random walk + fixed-cooldown basic attack. No targeting, no skills, no panel awareness.
- **Player death is still abrupt.** Win/lose flow is wired (`PlayState.checkBattleOver` → `GameOverScreen`), but only the enemy has a death fade. Player drop to 0 HP currently transitions immediately; a symmetric 0.5s fade on `Player` would feel cleaner.
- **No panel state transitions** (`NORMAL → CRACKED → BROKEN` on stand/leave).
- **Panel effects** (ICE slide, LAVA/POISON DoT, GRASS heal) are defined as `PanelType`s and have textures, but no behavioral hooks yet.
- **No multi-enemy support**; only a single `Enemy` instance baked into `PlayState`. A `List<Enemy>` and per-instance targeting in skills is the obvious refactor.
- **`SkillSelectOverlay` was tuned for 4 cards** but `HAND_SIZE = 6`; layout may overflow at 6. Worth a visual pass.
- **3D camera is fixed.** `GameEnvironment` poses the camera once at construction. No screen-shake hook yet — adding one means animating `cam3D.position`/`lookAt` and re-baking `BattleContext`'s projection cache (or invalidating it).
- **Fusion system parked.** Earlier design explored shape+element fusion tables; explicitly out of scope until the existing shapes feel good. Once it's revisited, fusion becomes "given two `Skill`s + their `Shape`/`Element`, produce a third or modify a spawned `SkillInstance`."
- **Main menu is still text-only** — two `font.draw` lines with no art, no buttons, no controller support (tap-to-start only). A proper title screen with selectable entries is future work.
