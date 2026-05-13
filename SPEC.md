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
│       ├── components/
│       │   ├── HitFlash.java          # flicker-on-hit timer (used by Player + Enemy)
│       │   └── InputLock.java
│       ├── entities/
│       │   ├── Battlefield.java       # 8x4 panel grid
│       │   ├── BattleVfx.java         # short-lived VFX interface
│       │   ├── ClashEffect.java       # short-lived VFX (also reused as skill VFX)
│       │   ├── Collider.java / CollisionResolver.java
│       │   ├── Enemy.java / Player.java
│       │   ├── Projectile.java
│       │   └── Team.java
│       ├── input/
│       │   ├── GameAction.java
│       │   ├── GamepadInputSource.java
│       │   ├── InputManager.java / InputSource.java
│       │   └── KeyboardInputSource.java
│       ├── screens/
│       │   ├── GameScreen.java
│       │   └── MainMenuScreen.java
│       ├── skills/
│       │   ├── ChargeMeter.java
│       │   ├── Skill.java / SkillInstance.java
│       │   ├── SkillFactory.java / SkillLibrary.java
│       │   ├── SkillCooldowns.java
│       │   ├── ShapeConfig.java / ProjectileConfig.java   # per-shape tuning data
│       │   ├── effects/Effect.java
│       │   ├── elements/Element.java
│       │   ├── instances/{Strike,Projectile,Beam,Aura,Zone}Instance.java
│       │   └── slots/{ButtonSlot,SkillSlots,SlotKey}.java
│       ├── state/
│       │   ├── GameScreenState.java
│       │   ├── PlayState.java
│       │   └── SkillSelectState.java
│       ├── systems/
│       │   ├── BattleContext.java
│       │   ├── CaveEnvironment.java    # 3D cave background + tile→screen projection
│       │   └── CombatSystem.java
│       ├── ui/
│       │   ├── ChargeBarHud.java
│       │   ├── FpsHud.java
│       │   ├── SkillSelectOverlay.java
│       │   └── SlotsHud.java
│       └── util/
│           ├── PanelGenerator.java
│           └── PositionSmoother.java
├── lwjgl3/                 # Desktop launcher
├── assets/                 # background, music, panels/, sprites/, attacks/, effects/
└── tools/                  # Offline asset-generation scripts (Python/Pillow)
```
## Architecture Notes
### Big picture
- **Pragmatic component-style**, not a real ECS. No Ashley/Artemis. `Player`/`Enemy` stay as classes but compose small component objects (currently just `InputLock`). New behavior goes through `systems/` classes (`CombatSystem`, `BattleContext`).
- **State machine** at the `GameScreen` level: `PlayState` ↔ `SkillSelectState`, both implementing `GameScreenState`. `GameScreen` owns the *persistent* per-battle data (skills, slots, cooldowns, charge); states own *transient* per-frame logic and assets they alone need.
- **State machine inside each `SkillInstance`**: every shape uses an inner `Phase` enum + `phaseTime` accumulator (e.g. `DASH_FORWARD → HIT → DASH_BACK` for Strike, `CHARGE → FIRE → FADE` for Beam, `APPEAR → ACTIVE → FADE` for Zone, etc.). Phase durations are tunable `private static final float`s at the top of each instance file.
- **3D-projected 2D rendering.** The battlefield is drawn as a 3D cave (`CaveEnvironment`, libGDX g3d `ModelBatch` + `Environment`) on the floor of which the panel grid lives. Each frame, entity/skill draws are placed using **tile→screen projections** the cave camera supplies (`projectTile`, `tileDepthScale`); back-row sprites render smaller. The 2D pass is captured by a `gdx-vfx` `VfxManager` and run through `BloomEffect` before going to the framebuffer.
- **No entity animation system yet.** Entity sprites are static PNGs; a frame-based controller is deferred. *Skill VFX* can be animated (`Skill.vfxAnimation` is an `Animation<TextureRegion>` — used by `ice_beam`'s sprite-sheet beam), but `Player`/`Enemy` themselves are still single sprites tweened by `PositionSmoother`.

### Package dependency rules
The deliberate import direction, top to bottom:
```
screens, state    →    systems, entities, ui, skills, input
ui                →    skills (read-only, for HUD)
skills            →    entities, components, systems   (skills know about the world)
systems           →    entities                        (systems coordinate entities)
entities          →    components, util                (entities use small helpers)
components, util  →    (leaf — nothing in this repo)
```
**`components` must not depend on `skills`.** That's why `InputLock.lock(Object)` takes `Object` rather than `SkillInstance` — identity is all that matters, and the back-edge would create a cycle.

### Per-frame loop
Every frame `GameScreen.render(delta)` runs:
1. `InputManager.update()` — refresh each child source, then OR-fold their pressed states. Edge detection (`isActionJustPressed`) lives at the manager so two sources holding the same action don't double-fire.
2. `currentState.input()` — dispatch `GameAction`s to handlers.
3. `currentState.update(delta)` — game logic (see "PlayState tick order" below).
4. `currentState.render(batch)` — clear, project, draw the world.
5. HUD pass — `ChargeBarHud` + `SlotsHud` always draw on top of whichever state is active.

**`PlayState.update` order matters.** It is, in sequence:
`tickEntities (player.update → enemy.update → re-project tile targets + depth scale onto entities) → tickMeters (charge + cooldowns) → enemyAi → tickProjectiles → combatSystem.update → resolveCollisions → tickAndCullEffects → cullDeadProjectiles`.
Skill instances thus tick *after* entity movement (so `originCol/Row` snapshotted at fire-time is still valid for that frame) and *before* collision resolution. The "re-project" step pushes each entity's projected screen-space target tile and depth scale into the entity so `PositionSmoother` tweens toward the right pixel and `render()` knows the perspective scale.

### Render layering
`PlayState.render(batch)` runs two passes inside a `VfxManager` capture, then post-FX to screen:

**Pass 1 — 3D cave (`cave3D.render`):** `ModelBatch` draws the cave shell (floor, walls, ceiling, stalactites) and the panel-grid slabs (blue on player side, red on enemy side) lit by `Environment` (ambient + directional + 4 point lights). The depth buffer is cleared afterwards so 2D never z-fights with 3D.

**Pass 2 — 2D battle (`renderWorld(batch)`), back to front:**
1. `combatSystem.renderUnder(batch)` — Zone effects only, drawn at floor level so the ground-plane art reads as terrain
2. Shadows — procedurally generated ellipse drawn at each entity's ground position
3. Entities, **Y-sorted** (higher `getVisualY()` draws first, so a player standing behind the enemy occludes correctly)
4. Basic-attack projectiles
5. `combatSystem.render(batch)` — every other active `SkillInstance` (beams, projectile sprites, auras, etc.)
6. battle VFX (`ClashEffect` and friends)

**Post-FX:** `VfxManager` ends capture, applies `BloomEffect` (base 1.0, bloom 1.2, threshold 0.25), and blits to screen.

**HUD pass** (`GameScreen.render`) draws `ChargeBarHud`, `SlotsHud`, and `FpsHud` on top of everything — outside the VFX capture, so HUD doesn't bloom.

`ZoneInstance.isRenderUnder()` is the single hook that splits combat-system draws across layers 1 and 5.

### Skill execution data flow
```
PlayState.handleSlotFire
   └─ tryFireSlot(key)
        ├─ player.isInputLocked() ?  → bail
        ├─ slot.pop()                → Skill (pure data)
        ├─ cooldowns.onUsed(skill)   → start its cooldown timer
        ├─ SkillFactory.create(skill, player)   → SkillInstance (behavior)
        └─ combatSystem.spawn(instance)         → registered for ticking
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
- `Battlefield` owns one texture per `PanelType`.
- `SkillLibrary` owns each skill's icon **and** its VFX texture (plus the sprite sheet backing any `Skill.vfxAnimation`); all disposed in `SkillLibrary.dispose()`.
- `CaveEnvironment` owns the cave wall/floor textures, `ModelBatch`, and every `Model` it builds.
- `PlayState` owns sprite sheets, basic-attack texture, clash texture, the procedural shadow texture, the `VfxManager`, the `BloomEffect`, and the `CaveEnvironment` instance.
- HUDs own their 1×1 pixel pixmaps (used for tinted rectangles).
- `GameScreen.dispose()` cascades into `playState`, `skillSelectState`, `chargeHud`, `slotsHud`, `skills`.
**Don't add `new Texture(...)` calls inside per-frame paths.** Construct in `*.create()`/state constructors, dispose alongside. The shadow texture is the canonical example — built once procedurally in `PlayState.buildShadowTexture()` and reused every frame.
## Core Classes
### `Main` (extends `Game`)
Application root. Owns shared rendering resources (`SpriteBatch batch`, `BitmapFont font`, `FitViewport viewport` — 16×9). `create()` pushes `MainMenuScreen`.
### Screens
- **`MainMenuScreen`** — splash; switches to `GameScreen` on touch.
- **`GameScreen`** — owns the per-battle persistent objects (`SkillLibrary skills`, `SkillSlots slots`, `SkillCooldowns cooldowns`, `ChargeMeter charge`) and HUDs (`ChargeBarHud`, `SlotsHud`, `FpsHud`). Delegates per-frame `input/update/render` to whichever `GameScreenState` is active; the HUD pass draws on top of the state's rendering.
### States
- **`PlayState`** — real-time combat. Reads movement (WASD), basic attack (J), slot fire (1/2/3), and the `Q+E` trigger combo to open the staging menu (gated on `charge.isFull()`). Owns `BattleContext` and `CombatSystem`, ticks projectiles, runs `CollisionResolver`, drives the cooldown table.
- **`SkillSelectState`** — staging menu. `enter()` snapshots all slots, draws a hand of 6 (filtered by `cooldowns` + slots already loaded). Cursor navigation is `MOVE_LEFT/RIGHT`; `SKILL_X/Y/B` assign the highlighted card to that slot; `Enter` confirms (drains charge); `Esc`/`Back` cancels (slots restored from snapshot, charge preserved). Renders the world frozen behind the overlay.
### Entities
- **`Battlefield`** — `COLS=8 × ROWS=4` grid of `PanelType` enum values. Cols 0–3 are player territory, cols 4–7 enemy. Panel types: `NORMAL_BLUE`, `NORMAL_RED`, `CRACKED`, `BROKEN`, `ICE`, `LAVA`, `GRASS`, `POISON`. `PanelGenerator` has two factories: `generatePanels()` (default flat blue/red split, used today) and `generateMixedPanels()` (sprinkles hazard tiles — useful for testing once panel effects are wired).
- **`Player`** — one cell `(col,row)`, hp 100, half-grid clamped on `moveLeft/Right` (`col ∈ [0, COLS/2 - 1]`). Composes an `InputLock` (skills lock the body during dash/cast) and a `HitFlash` (300ms post-hit invisible-frame flicker). `forceSetTile(col,row)` bypasses the half-grid clamp so a `StrikeInstance` can drive the player into enemy territory mid-skill. `takeDamage(int)` reduces `hp` and calls `hitFlash.flash()`. `setProjectedTarget(x,y)` + `setDepthScale(s)` are pushed by `PlayState.tickEntities` each frame so `PositionSmoother` tweens toward the projected screen position and `render()` scales the sprite by row depth.
- **`Enemy`** — mirror, constrained to cols 4–7 (`col ∈ [COLS/2, COLS - 1]`), hp 100. Composes a `HitFlash`. No `InputLock` because enemies don't cast skills yet. **Has a placeholder AI now**: `stepRandomly()` picks a random direction every 0.5–1.5s; basic attack fires whenever its `basicAttackCooldown` (1.25s) is up. **Status & death are wired**: `applyFreeze(duration)` halts movement/attack timers and tints the sprite blue (`FREEZE_TINT`); `isFrozen()` is the gate. When `hp <= 0` the enemy enters a 0.5s death fade (`isDying()` → sprite + shadow alpha lerp); `isDead()` reports when it's safe to remove.
- **`Projectile`** — world-space, lives outside the grid (just position + velocity). Off-screen exits cull it.
- **`Collider` / `Team` / `CollisionResolver`** — `Collider` is the small interface (`getBounds`, `getTeam`, `isAlive`) implemented by `Player`, `Enemy`, and `Projectile`. `CollisionResolver.resolve` runs two passes: (A) opposing projectiles cancel each other and append a clash midpoint to the supplied list; (B) surviving projectiles flash any opposing entity they overlap. **Pass A first** is intentional — it lets a projectile that would have hit an entity get canceled by an opposing projectile on the same frame.
- **`BattleVfx`** (interface) / **`ClashEffect`** — `BattleVfx` is `update + render + isAlive`. `ClashEffect` scales 0.6→1.6 and fades over 0.25s; `PlayState` culls when `!isAlive()`.
### Skills
- **`Skill`** — pure, immutable data: `id`, `displayName`, `description`, `icon`, `Shape` (enum: `PROJECTILE/BEAM/STRIKE/AURA/ZONE`), `Element`, `List<Effect>` (unmodifiable), `cooldown`, **required** `vfxTexture`, optional `vfxAnimation` (`Animation<TextureRegion>` — when present, renderers prefer it over the static texture; this is how `ice_beam` animates), optional `ShapeConfig`. The same `Skill` can spawn many simultaneous `SkillInstance`s. Convenience constructors omit the trailing `vfxAnimation`/`shapeConfig` for skills that don't need them.
- **`ShapeConfig` / `ProjectileConfig`** — marker interface + per-shape tuning record. `ProjectileConfig` is the only concrete one today: `straight(speed)` for laser-style movement, `lob(targetRange, arcHeight)` for arcing tosses that land after `LOB_FLIGHT_TIME`. Add a new `ShapeConfig` impl when a shape grows tunable parameters.
- **`SkillInstance`** — abstract base for runtime executions. Tracks `caster`, snapshotted origin tile (`originCol/originRow` captured at construction), `finished/lockTaken` flags. Methods: `acquireInputLock()`, `releaseInputLock()`, `finish()`, abstract `update(delta, BattleContext)`, default no-op `render(batch, ctx)`.
- **`SkillFactory.create(Skill, Player)`** — single static switch on `Shape` returning the right `SkillInstance` subclass. Adding a new shape means adding an enum entry, a switch case, and a new `SkillInstance` subclass.
- **`SkillLibrary`** — currently defines seven starter skills:
    - `wind_strike` — STRIKE, 15 dmg, 2.0s cd
    - `fire_blast` — PROJECTILE/straight (8 u/s), 20 dmg, 3.0s cd
    - `venom_bomb` — PROJECTILE/lob (range 2, arc 2.0), 12 dmg + spawns a `ZoneInstance` "cloud" on landing, 3.5s cd
    - `ice_beam` — BEAM, 25 dmg **+ 2.0s FREEZE @ 100% chance**, 4.0s cd; uses an `Animation<TextureRegion>` sliced from `skills/animations/icebeam_spritesheet.png` (0.1s frame time)
    - `frost_trap` — ZONE/Ice, 5 dmg + 2.0s FREEZE @ 100%, 3.5s cd
    - `heal` — AURA, 5.0s cd (per-tick effect TODO)
    - `shield` — AURA, 4.0s cd (per-tick effect TODO)
  `drawHand(n, cooldowns, slots)` filters out skills currently on cooldown OR already loaded into any slot, shuffles, and takes `n`.
- **`SkillCooldowns`** — `Map<String, Float>` remaining time. `update`, `onUsed`, `isOnCooldown`, `remainingFor`, `clear`.
- **`ChargeMeter`** — fills over time (max=1, fillRate=0.05/s ≈ 20s). `isFull()` gates the staging menu; `consume()` drains on confirm; `add(amount)` for future "charge on kill / on hit" rewards.
- **`StrikeInstance`** — Phases: `DASH_FORWARD` (0.10s) → `HIT` (0.20s) → `DASH_BACK` (0.10s) → `DONE`. Acquires the player's `InputLock`, snaps to `originCol+1` via `forceSetTile`, spawns a `ClashEffect` on the target tile (`originCol+2`), applies `DAMAGE`-typed effects to the enemy if it occupies the target tile, then snaps back. Status-effect TODOs (`POISON/BURN/STUN/FREEZE`) are flagged inline.
- **`ProjectileInstance`** — Two movement modes via `ProjectileConfig`:
    - `STRAIGHT`: spawns at `caster + 1 panel` and travels right at `config.speed`; finishes when it crosses the right edge of the grid or overlaps the enemy's tile center on the same row.
    - `LOB`: parabolic arc from caster to `originCol + targetRange` over `LOB_FLIGHT_TIME` (0.50s); on landing applies damage if the enemy is on the landing tile, then spawns a child `ZoneInstance` (the "cloud") via `ctx.combatSystem.spawn(...)` for lingering effect.
  Does **not** acquire `InputLock` — the caster keeps playing while their projectile flies.
- **`BeamInstance`** — Phases: `CHARGE` (0.20s) → `FIRE` (0.70s) → `FADE` (0.25s) → `DONE`. Acquires `InputLock` for the windup; releases at the start of `FADE`. Hits everything on the caster's row from `originCol+1` rightward exactly once on entering `FIRE`. Render: if `Skill.vfxAnimation` is present (e.g. `ice_beam`'s sprite-sheet) the beam draws the current `Animation` frame; otherwise it stretches the static `vfxTexture`. During `CHARGE` the beam extends out from the caster (`w = fullW * (phaseTime/CHARGE)`); during `FADE` alpha lerps to zero.
- **`AuraInstance`** — Phases: `EXPAND` (0.20s) → `ACTIVE` (3.00s) → `FADE` (0.20s) → `DONE`. Renders a scaled, pulsing sprite on the caster's tile. `applyTick(ctx)` runs every `TICK_INTERVAL` (0.50s) but is a TODO until self-targeted effects exist.
- **`ZoneInstance`** — Phases: `APPEAR` (0.15s) → `ACTIVE` (1.00s) → `FADE` (0.25s) → `DONE`. Pinned to a target tile (defaults to `caster +1, sameRow`; can be overridden — that's how `ProjectileInstance` plants a cloud on its landing tile). Damages whatever opposing entity is standing on the tile every `TICK_INTERVAL` (0.33s). Returns `true` from `isRenderUnder()`, so it draws under entities (terrain feel).
- **`slots/`** — `SlotKey` (`X, Y, B`), `ButtonSlot` (FIFO `ArrayDeque<Skill>`, `CAPACITY=2`), `SkillSlots` (`EnumMap<SlotKey, ButtonSlot>` + `contains(Skill)`). Slots persist across `PlayState` ↔ `SkillSelectState` transitions; cancel restores from a snapshot taken on `enter()`.
- **`effects/Effect`** — `Type` enum (`DAMAGE, APPLY_POISON, APPLY_BURN, STUN, FREEZE`), with `value`, `duration`, `chance` fields. `Effect.damage(int)` helper for the common case. `SkillInstance.applyEffectsTo(Enemy)` consumes `DAMAGE` (deals `value`) and `FREEZE` (rolls `chance` against 0–99, then calls `Enemy.applyFreeze(duration)`). `APPLY_POISON`, `APPLY_BURN`, and `STUN` are still reserved.
- **`elements/Element`** — `NONE, FIRE, POISON, ICE, LIGHTNING`. Currently cosmetic — no element-vs-element resistance/weakness logic yet.
### Systems
- **`BattleContext`** — bag of references (battlefield, player, enemy, `List<BattleVfx> vfx`, `CaveEnvironment cave3D`, *and* a back-reference to its owning `combatSystem` set immediately after construction). Constructed once in `PlayState`. Projection helpers `projectedTileWorld(col,row)` (tile center in 2D viewport space) and `tileDepthScale(row)` (1.0 near → ~0.78 far) are how skill instances place their VFX correctly on the 3D-projected floor. `buildCache()` (called from `PlayState.resize`) bakes a `COLS×ROWS` lookup so every per-frame skill draw is a constant-time array hit, not a `cam3D.project()` call. Pass-around context (not a service locator) so skills can be unit-tested with fakes.
- **`CombatSystem`** — owns `List<SkillInstance> active`.
    - `spawn(instance)` adds to the active list (callable from anywhere with a `BattleContext`, including from inside another `SkillInstance.update`).
    - `update(delta)` ticks every active instance over a snapshot index — instances spawned mid-tick join the list but won't be ticked until next frame.
    - `renderUnder(batch)` draws zone instances flagged `isRenderUnder()`; `render(batch)` draws everything else. Two-pass rendering is what lets zone tiles sit visually beneath player/enemy sprites.
    - `hasActive()` for future menu/pause logic.
- **`CaveEnvironment`** — the 3D-rendered cave that hosts the battlefield. Owns a `PerspectiveCamera` (45° FOV, positioned high and tilted ~40° below horizontal), an `Environment` with ambient + directional + four point lights tinted to match the crystal palette, and a `ModelBatch` driving `ModelInstance`s built once in `buildGeometry()` (floor slab, back wall, ceiling, side walls, eight stalactites, and the COLS×ROWS panel grid — blue diffuse + blue-specular on the player side, red on the enemy side, so the point lights cast tinted glows on each tile). Exposes two helpers consumed by `BattleContext`: `projectTile(col,row)` maps a tile center on the 3D floor into 2D viewport world coordinates so sprites can be drawn standing on it, and `tileDepthScale(row)` returns a linear scale `1.0 → 1.0 - DEPTH_SCALE_FAR (0.22)` from near to far row. `dispose()` releases all built `Model`s plus the wall/floor textures and the `ModelBatch`.
### Components
- **`InputLock`** — owner-based lock. `lock(owner)` refuses if a different owner holds it; `unlock(owner)` is a no-op if you're not the holder. Owner is `Object` (not `SkillInstance`) so `components` doesn't depend on `skills`. Identity comparison is all that matters.
- **`HitFlash`** — post-hit flicker timer (0.3s total, 0.05s flicker interval). `flash()` arms; `tick(delta)` decays; `isHidden()` returns true on alternating intervals while armed. `Player.render` and `Enemy.render` skip the draw when `isHidden()` is true, producing the invisible-frame strobe. Replaced the inline timer the entities used to keep themselves.
### UI
- **`ChargeBarHud`** — bottom-screen charge meter, batch-aware (`wasDrawing` check).
- **`SlotsHud`** — bottom-right X/Y/B columns with FIFO icons (top of column = front of queue), placeholder for empty cells.
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
| J | `ATTACK_BASIC` | Spawn wind-slash projectile |
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
The `SKILL_X/Y/B` actions are dual-purpose by state: in `PlayState` they fire the front of the slot, in `SkillSelectState` they assign the highlighted hand card. Gamepad cancel is on Back rather than B so pressing B inside the menu doesn't double-fire (assign + cancel).
## Skill Flow (MVP)
1. Wait ~20s for the `ChargeMeter` to fill.
2. Hold Q+E (or L2+R2) → `GameScreen` flips to `SkillSelectState`, world freezes, hand of 6 drawn.
3. ←/→ to move the cursor; 1/2/3 (or X/Y/B) to assign the highlighted card to that slot. Slots that were already loaded persist; the menu is "top up," not "rebuild."
4. Enter to confirm (drains charge, returns to `PlayState`) or Esc/Back to cancel (slots restored from snapshot, charge kept).
5. In play, 1/2/3 (or X/Y/B) fires the front of that slot via `SkillFactory.create()` → `CombatSystem.spawn()`. The skill's cooldown starts.
## Design Rationale
- **Charge gates the menu, cooldowns gate the hand.** Two coexisting timers with different jobs: `ChargeMeter` controls when you can stage, per-skill cooldowns control which skills are *available* to stage or fire.
- **Slots persist across menu opens** (Battle Network "custom screen" feel).
- **Snapshot/restore on cancel** rather than journaling individual assigns. Simpler and player-indistinguishable.
- **`InputLock` owner is `Object`** so the `components` package doesn't depend on `skills`. Identity comparison is all that matters.
- **`forceSetTile` bypasses the half-grid clamp** because a Strike's HIT phase puts the player in enemy territory. The clamp on `moveLeft/Right` is correct for normal movement and wrong for skills.
- **VFX reuses `ClashEffect`** because it already does scale+fade over a short lifetime — exactly the slash flourish we want. Per-skill VFX textures live on `Skill.vfxTexture` and are required non-null — every skill must supply one.
- **Hit resolution lives inside each `SkillInstance`** (synchronous, fixed-timing). `CollisionResolver` is reserved for basic-attack projectiles; skill projectiles do their own tile-center overlap test in `ProjectileInstance.checkHitStraight`. Unification is a future refactor (see Known Gaps).
- **Child instances spawn via `ctx.combatSystem.spawn(...)`.** A `ProjectileInstance` LOB landing plants a `ZoneInstance` cloud this way. Avoid coupling instances directly — always go through the combat system so spawn order, layering, and reaping stay consistent.
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
- **Status effects partially wired.** `applyEffectsTo` consumes `DAMAGE` and `FREEZE` (the latter halts enemy movement/attack and tints the sprite). `APPLY_POISON`, `APPLY_BURN`, and `STUN` are still reserved — they need a `StatusComponent`-style holder on entities, applied through the same effects loop.
- **Basic attacks still don't deal damage**, just flash. `CollisionResolver` Pass B calls `flash()` but not `takeDamage()`. Easy fix once basic-attack projectiles carry a damage payload.
- **Skill projectiles bypass `CollisionResolver`.** `ProjectileInstance` does its own per-tile overlap test in `checkHitStraight`. Two collision systems is fine for now but will want unification (and projectile-vs-projectile cancels for skill projectiles) once there's more than one shape that fires through the air.
- **No I-frames during Strike.** Player can be hit while dashing. Simplest fix: an invulnerable flag the resolver checks.
- **Enemy doesn't use skills.** No `InputLock` on `Enemy`. Mirror the `Player` additions when enemies need to cast.
- **Enemy AI is a placeholder** — random walk + fixed-cooldown basic attack. No targeting, no skills, no panel awareness.
- **Death exists, win/lose doesn't.** `Enemy` has a 0.5s death fade (`isDying`/`isDead`) but nothing reads `Player.isAlive()` or schedules a game-over screen. Player death is also still abrupt — no symmetric fade.
- **No panel state transitions** (`NORMAL → CRACKED → BROKEN` on stand/leave).
- **Panel effects** (ICE slide, LAVA/POISON DoT, GRASS heal) are defined as `PanelType`s and have textures, but no behavioral hooks yet.
- **No multi-enemy support**; only a single `Enemy` instance baked into `PlayState`. A `List<Enemy>` and per-instance targeting in skills is the obvious refactor.
- **`SkillSelectOverlay` was tuned for 4 cards** but `HAND_SIZE = 6`; layout may overflow at 6. Worth a visual pass.
- **3D camera is fixed.** `CaveEnvironment` poses the camera once at construction. No screen-shake hook yet — adding one means animating `cam3D.position`/`lookAt` and re-baking `BattleContext`'s projection cache (or invalidating it).
- **Fusion system parked.** Earlier design explored shape+element fusion tables; explicitly out of scope until the existing shapes feel good. Once it's revisited, fusion becomes "given two `Skill`s + their `Shape`/`Element`, produce a third or modify a spawned `SkillInstance`."
- **Main menu title still reads "Drop~!!!"** (leftover from scaffold template).
