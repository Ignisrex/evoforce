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
│       │   ├── Caster.java            # SkillDeck + SkillSlots + basicAttack + team + InputLock
│       │   ├── GridPosition.java      # col/row + PositionSmoother + projected target/depth-scale
│       │   ├── GridMovement.java      # bundles GridPosition + GridBounds (battle entity body)
│       │   ├── GridBounds.java        # per-entity legal tile range (player 0-3, enemy 4-7)
│       │   ├── Direction.java         # UP/DOWN/LEFT/RIGHT step vectors (dCol/dRow)
│       │   ├── FreePosition.java      # continuous floor x/z + speed + room bounds (overworld avatar)
│       │   ├── Health.java            # current/max HP (mutated only via DamageSystem)
│       │   ├── Stats.java             # power/magic/vitality/defense/speed (caster stat block)
│       │   └── Team.java              # PLAYER / ENEMY (read everywhere team matters)
│       ├── entities/
│       │   ├── Battlefield.java       # 8×4 panel grid + 3D floor placement (floorX/floorZ)
│       │   ├── BattleVfx.java         # short-lived VFX interface
│       │   ├── ClashEffect.java       # short-lived VFX (also reused as skill VFX)
│       │   ├── Player.java            # implements Combatant; composes (injected) Caster + GridMovement + Health + Stats + StatusContainer
│       │   └── Enemy.java             # implements Combatant; same composition + simple AI
│       ├── environment/                # HD-2D rendering
│       │   ├── GameEnvironment.java    # 3D cave room (ModelBatch) + decoration list; grid-agnostic
│       │   ├── SceneCamera.java        # perspective camera + continuous project(x,z) / depthScale(z)
│       │   └── BattlefieldDecor.java   # builds the battle floor panels into a GameEnvironment
│       ├── input/
│       │   ├── GameAction.java
│       │   ├── GamepadInputSource.java
│       │   ├── InputManager.java / InputSource.java
│       │   └── KeyboardInputSource.java
│       ├── screens/
│       │   ├── GameOverScreen.java
│       │   ├── GameScreen.java         # battle screen; runs PlayState ↔ SkillSelectState
│       │   ├── MainMenuScreen.java
│       │   ├── OverworldScreen.java    # free-roam scene; door floor-rects launch battles
│       │   └── state/
│       │       ├── GameScreenState.java
│       │       ├── PlayState.java
│       │       └── SkillSelectState.java
│       ├── sessions/                   # run-level state (held on Main, outlives a battle)
│       │   ├── GameSession.java        # SkillLibrary + PlayerProfile
│       │   └── PlayerProfile.java      # persistent player identity: Caster + Stats
│       ├── skills/
│       │   ├── ChargeMeter.java
│       │   ├── Skill.java / SkillInstance.java
│       │   ├── SkillFactory.java / SkillLibrary.java / SkillLoader.java
│       │   ├── SkillDeck.java                            # per-caster skill pool + cooldowns
│       │   ├── SkillCooldowns.java                       # internal to SkillDeck
│       │   ├── ShapeConfig.java / ProjectileConfig.java   # per-shape tuning data
│       │   ├── effects/
│       │   │   ├── Effect.java                          # immutable effect record (DAMAGE/HEAL/APPLY_STATUS/KNOCKBACK)
│       │   │   └── EffectType.java
│       │   ├── elements/Element.java
│       │   ├── instances/{Strike,Projectile,Beam,Aura,Zone}Instance.java
│       │   └── slots/{ButtonSlot,SkillSlots,SlotKey}.java
│       ├── systems/
│       │   ├── BattleContext.java     # references + tile-projection cache + combatant lookups
│       │   ├── CombatSystem.java      # ticks SkillInstances + status containers, resolves clashes
│       │   ├── MovementSystem.java    # sole position writer: grid tryGridStep + free applyFreeInput
│       │   └── combat/
│       │       ├── Combatant.java     # interface; Player & Enemy implement it
│       │       ├── DamageSystem.java  # single entry point for HP mutation (defense, triggers, death)
│       │       ├── TriggerBus.java    # pub/sub on Trigger enum (with per-combatant filtering)
│       │       ├── Trigger.java / TriggerListener.java
│       │       ├── Status.java        # base: duration, optional tick interval, block flags
│       │       ├── StatusContainer.java # per-combatant status slots (EnumMap<StatusType, Status>)
│       │       ├── StatusFactory.java / StatusType.java
│       │       ├── event/{DamageEvent,HealEvent,TriggerEvent}.java
│       │       └── status/{Freeze,Burn,Poison,Stun,Regen,Shield}Status.java
│       ├── ui/
│       │   ├── BasicAttackHud.java
│       │   ├── ChargeBarHud.java
│       │   ├── FpsHud.java
│       │   ├── LifeBarHud.java
│       │   ├── SkillSelectOverlay.java
│       │   └── SlotsHud.java
│       └── util/                       # leaf helpers (no upward deps)
│           ├── HitFlash.java
│           ├── InputLock.java
│           ├── PanelGenerator.java
│           └── PositionSmoother.java
├── lwjgl3/                 # Desktop launcher
├── assets/                 # background, music, panels/, sprites/, attacks/, effects/, skills/
└── tools/                  # Offline asset-generation scripts (Python/Pillow + PixelLab REST API)
```
## Architecture Notes
### Big picture
- **Pragmatic component-style**, not a real ECS. No Ashley/Artemis. `Player` and `Enemy` are concrete classes that both **implement `Combatant`** and compose role components from `components/` (`Caster` — `SkillDeck`/`SkillSlots`/`basicAttack`/`team`/`InputLock`; `GridPosition` — `col`/`row`/`PositionSmoother`/projected-target/depth-scale; `Health` — current/max HP; `Stats` — power/magic/vitality/defense/speed) plus a per-entity `StatusContainer` (in `systems/combat/`). Leaf helpers (`HitFlash`, `InputLock`, `PositionSmoother`) live in `util/`. New behavior either composes another component or is added under `systems/` (top-level — `CombatSystem`, `BattleContext`) or `systems/combat/` (combat-pipeline — `DamageSystem`, `TriggerBus`, statuses).
- **`Combatant` interface is the skill-side handle on an entity.** Every `SkillInstance` operates on a `Combatant`, not on `Player`/`Enemy` directly — so the same skill code aims at either team. `Combatant` exposes the component getters (`getHealth`, `getStats`, `getCaster`, `getGridPosition`, `getGridMovement`, `getTeam`, `getStatusContainer`), the position shortcuts (`getCol/Row`, `getVisualX/Y`, `getDepthScale`), and lifecycle callbacks (`onHitFlash`, `onDeath`) the damage pipeline invokes.
- **HP only mutates through `DamageSystem`.** Direct `Health.damage(...)`/`heal(...)` calls live only inside `DamageSystem`. Every skill, status DoT, and panel hazard fans through `DamageSystem.apply(DamageEvent)` / `.heal(HealEvent)`, which applies defense, fires the `ON_DAMAGE_TAKEN_PRE` trigger (shields zero damage here), mutates HP, then fires `ON_DAMAGE_TAKEN`/`ON_HIT_LAND`/`ON_DEATH`/`ON_KILL` in order.
- **Status effects are uniform across player and enemy.** Each `Combatant` owns a `StatusContainer` keyed by `StatusType`. A skill applies `APPLY_STATUS` → the container holds a `Status` instance which ticks every frame (DoT/HoT via `onTick`), exposes `blocksMovement/Attack/Casting` flags consulted by movement input + AI, and can subscribe to the `TriggerBus` in `onApply` for reactive effects (`ShieldStatus` is the canonical example).
- **State machine** at the `GameScreen` level: `PlayState` ↔ `SkillSelectState`, both implementing `GameScreenState` (under `screens/state/`). `GameScreen` owns the per-battle `ChargeMeter charge` + HUDs and reads the run off `Main.session`; the `SkillLibrary` and persistent player loadout live on `GameSession` (in `sessions/`), not on `GameScreen`. Per-caster state (`SkillDeck`, `SkillSlots`, `basicAttack`, position, HP, stats, statuses) lives on the entity's components. States own *transient* per-frame logic and assets they alone need.
- **Run-level state lives on `Main.session` (`GameSession`).** A run owns the `SkillLibrary` plus a persistent `PlayerProfile` (`Caster` + `Stats`) that survives across battles. The battle `Player` is built *from* the profile each battle (HP refills); the overworld avatar is a separate lightweight body sharing the same identity. This is why `Player`'s constructor takes a prebuilt `Caster`/`Stats` rather than minting its own.
- **Top-level Screen flow** (peer to the in-battle state machine): `MainMenuScreen` → `OverworldScreen` → `GameScreen` (battle), then **win → `OverworldScreen`** / **loss → `GameOverScreen` → `OverworldScreen`** (restart). The overworld is a free-roam scene (no panels); walking the avatar into a door floor-rect launches a battle. `Main.setScreen` is overridden to dispose the predecessor via `Gdx.app.postRunnable` so any caller — including code running inside the predecessor's own `render` — can swap screens without freeing textures mid-frame.
- **State machine inside each `SkillInstance`**: every shape uses an inner `Phase` enum + `phaseTime` accumulator (e.g. `DASH_FORWARD → HIT → DASH_BACK` for Strike, `CHARGE → FIRE → FADE` for Beam, `APPEAR → ACTIVE → FADE` for Zone/Aura, etc.). Phase durations are tunable `private static final float`s at the top of each instance file.
- **3D-projected 2D (HD-2D) rendering, one environment for both scenes.** `GameEnvironment` (package `environment/`) renders a 3D cave shell via libGDX g3d `ModelBatch` + `Environment`; 2D sprites are billboards placed onto the floor. It is **grid-agnostic**: `SceneCamera` owns the camera + continuous `project(worldX, worldZ)` / `depthScale(worldZ)`; `Battlefield` owns the grid's floor placement (`floorX`/`floorZ`); battle panels are floor *decoration* added via `BattlefieldDecor`. `BattleContext` bakes a tile→screen cache (`projectedTileWorld`/`tileDepthScale`) from `battlefield.floor*` + `environment.project/depthScale`. The overworld uses the same `GameEnvironment` with no panels, projecting its avatar's continuous `FreePosition`. The battle 2D pass is captured by a `gdx-vfx` `VfxManager` and run through `BloomEffect`.
- **No entity animation system yet.** Entity sprites are static PNGs; a frame-based controller is deferred. *Skill VFX* can be animated (`Skill.vfxAnimation` is an `Animation<TextureRegion>` — used by `ice_beam`'s sprite-sheet beam), but `Player`/`Enemy` themselves are still single sprites tweened by `PositionSmoother`.

### Package dependency rules
The deliberate import direction, top to bottom:
```
Main                    →  screens, sessions, assets, registry
screens, screens.state  →  systems, systems.combat, environment, entities, components, sessions, ui, skills, input, registry
assets                  →  registry, components            (GameAssets loads monster sprites via Monster.texturePath(Team))
registry                →  assets, components              (MonsterRegistry fetches from GameAssets; Monster owns the sprite path)
ui                      →  skills, entities                (read-only, for HUD)
skills                  →  entities, components, util, systems, systems.combat   (skills act on Combatants and route HP through DamageSystem)
skills.effects          →  systems.combat                  (Effect carries a StatusType payload)
systems                 →  entities, components, systems.combat, environment   (BattleContext holds a GameEnvironment; CombatSystem ticks status containers)
systems.combat          →  components                      (Combatant exposes component getters; statuses fan out HealEvent/DamageEvent)
environment             →  entities, components            (BattlefieldDecor reads Battlefield; SceneCamera/GameEnvironment are otherwise gdx-only)
sessions                →  skills, components              (PlayerProfile owns Caster + Stats, seeds deck from SkillLibrary)
entities                →  components, systems.combat, util  (Player/Enemy implement Combatant; compose StatusContainer)
components              →  skills, util, entities          (Caster imports SkillDeck; GridPosition imports Battlefield)
util                    →  (leaf — InputLock, HitFlash, PositionSmoother, PanelGenerator)
```
**`util` is leaf.** Helpers like `InputLock` and `HitFlash` live there because they're skill-agnostic — `InputLock.lock(Object)` takes `Object` so a `SkillInstance` can hold the lock without `util` depending on `skills`. Identity comparison is the contract.

**`Team` lives in `components/`, not `entities/`.** It's read by `Caster`, by `ProjectileInstance` (for direction), and by every `BattleContext` team-aware lookup. Keeping it under `components/` lets `Caster` import it without creating a `components/ → entities/` edge for an enum alone.

**`systems.combat/` is the combat-pipeline subpackage.** It holds the `Combatant` interface, `DamageSystem`, `TriggerBus`, and the `Status` hierarchy. It depends *only* on `components/` — no upward edge into `entities/` or `skills/` — so the pipeline can be unit-tested with fakes. The `entities → systems.combat` edge exists because `Player`/`Enemy` implement `Combatant`; the `skills → systems.combat` edge exists because `SkillInstance` operates on `Combatant`s and routes HP changes through `DamageSystem`.

**`components/ → entities/`** still exists only because `GridPosition` needs `Battlefield` for tile-center lookups. Tolerable: `entities/Battlefield.java` is itself a leaf (pure geometry + panel state, no upward deps). If we tighten the cycle, move `Battlefield` somewhere neutral.

**`assets/ ↔ registry/` is a tolerated cycle.** `GameAssets` loads monster sprites (it iterates the `Monster` enum and calls `Monster.texturePath`), while `MonsterRegistry` fetches them back through `GameAssets` — so the two packages import each other. Accepted because the only thing crossing the boundary is the monster sprite-path convention; the alternative (a third neutral package just for `Monster`) buys little for two small classes. If it grows, move `Monster` somewhere neutral both can depend on.

**`components/` is the ECS-style role layer.** Skill-aware composable pieces live here and may freely import from `skills`. Today: `Caster`, `GridPosition`, `GridMovement`, `GridBounds`, `Direction`, `FreePosition`, `Health`, `Stats`, `Team`. `Health` and `Stats` are deliberately dumb data — all mutation logic lives in `systems/combat/DamageSystem`. Entities reach skill state through the composed `Caster`, not through a direct `entities → skills` import. `GridMovement` bundles a `GridPosition` with the entity's `GridBounds`; `Direction` is the shared step-vector enum read by `MovementSystem`.

### Per-frame loop
Every frame `GameScreen.render(delta)` runs:
1. `InputManager.update()` — refresh each child source, then OR-fold their pressed states. Edge detection (`isActionJustPressed`) lives at the manager so two sources holding the same action don't double-fire.
2. `currentState.input()` — dispatch `GameAction`s to handlers.
3. `currentState.update(delta)` — game logic (see "PlayState tick order" below).
4. `currentState.render(batch)` — clear, project, draw the world.
5. HUD pass — `ChargeBarHud` + `SlotsHud` + `BasicAttackHud` + `LifeBarHud` + `FpsHud` always draw on top of whichever state is active.

**`PlayState.update` order matters.** It is, in sequence:
`tickEntities (player.update → each enemy.update → re-project tile targets + depth scale onto entities) → tickMeters (charge only — per-caster cooldowns tick inside each entity's update via the composed Caster's update → deck.update) → enemyAi (loop over all enemies) → combatSystem.tickStatuses (advance per-combatant StatusContainers — DoTs/HoTs can damage or heal here, possibly killing the owner) → combatSystem.update (skill instances tick + clash resolution + finished cull) → checkBattleOver (early-exit on win/lose) → tickAndCullEffects`.
Status ticks happen *between* enemy AI and skill instance updates so a status DoT that kills an enemy this frame propagates into the same frame's clash/hit checks (which test `isAlive()`). Skill instances tick *after* entity movement (so `originCol/Row` snapshotted at fire-time is still valid for that frame). The "re-project" step pushes each entity's projected screen-space target tile and depth scale into its `GridPosition` so the smoother tweens toward the right pixel and `render()` knows the perspective scale. `checkBattleOver` reads `allEnemiesDead()` (every enemy in the list past its 0.5s death fade) and `!player.isAlive()`; on either trigger it sets `transitionScheduled = true` and calls `Main.setScreen(new GameOverScreen(...))`. The flag short-circuits subsequent ticks so the transition only fires once.

### Render layering
`PlayState.render(batch)` runs two passes inside a `VfxManager` capture, then post-FX to screen:

**Pass 1 — 3D scene (`environment.render`):** `ModelBatch` draws the scene shell (floor, walls, ceiling, stalactites — currently themed as a cave) and the panel-grid slabs (blue on player side, red on enemy side) lit by `Environment` (ambient + directional + 4 point lights). The depth buffer is cleared afterwards so 2D never z-fights with 3D.

**Pass 2 — 2D battle (`renderWorld(batch)`), back to front:**
1. `combatSystem.renderUnder(batch)` — Zone effects only, drawn at floor level so the ground-plane art reads as terrain
2. Shadows — procedurally generated ellipse drawn at each entity's ground position
3. Entities, **Y-sorted** across the player and every alive enemy (higher `getVisualY()` draws first, so a back-row caster occludes correctly regardless of how many enemies are on the field)
4. `combatSystem.render(batch)` — every active `SkillInstance` (beams, projectile sprites including both casters' basic attacks, auras, etc.)
5. battle VFX (`ClashEffect` and friends)

**Post-FX:** `VfxManager` ends capture, applies `BloomEffect` (base 1.0, bloom 1.2, threshold 0.25), and blits to screen.

**HUD pass** (`GameScreen.render`) draws `ChargeBarHud`, `SlotsHud`, `BasicAttackHud`, `LifeBarHud`, and `FpsHud` on top of everything — outside the VFX capture, so HUD doesn't bloom.

`ZoneInstance.isRenderUnder()` is the single hook that splits combat-system draws across layers 1 and 5.

### Skill execution data flow
```
PlayState.handleSlotFire
   └─ tryFireSlot(key)
        ├─ player.isInputLocked() || statusContainer.blocksMovement() ?  → bail
        ├─ slot.pop()                       → Skill (pure data)
        ├─ deck.onUsed(skill)               → start its cooldown timer
        ├─ SkillFactory.create(skill, player)  → SkillInstance (behavior; pulls Caster + GridPosition off the Combatant)
        └─ combatSystem.spawn(instance)     → registered for ticking
```
- `Skill` is reusable, immutable data: `Shape`, `Element`, `cooldown`, ordered `List<Effect>`, mandatory `vfxTexture`, optional animation + `ShapeConfig`, plus `powerScale`/`magicScale` knobs that mix the caster's `Stats.power`/`Stats.magic` into the damage roll. Many `SkillInstance`s can run from the same `Skill`.
- `SkillInstance` owns its own state machine; subclasses override `update(delta, ctx)` and optionally `render(batch, ctx)`. The base class holds the firing `Combatant` (plus its `caster`/`pos` for convenience), tracks `originCol/Row` snapshotted at construction, and wires `acquireInputLock()` / `releaseInputLock()` / `finish()` boilerplate.
- Hit application goes through `applyEffectsTo(Combatant, BattleContext)` on the base class — one generic method handling `DAMAGE` (routed through `ctx.damageSystem.apply` with `powerScale/magicScale` mixed into the base value), `HEAL` (`ctx.damageSystem.heal`), `APPLY_STATUS` (rolls `chance`, then `target.getStatusContainer().apply(StatusFactory.create(...), ctx.triggerBus)`), and the reserved `KNOCKBACK`. There are no per-entity overloads — Player vs Enemy is just a flag the `DamageSystem` and `StatusContainer` honor uniformly.
- A skill instance can spawn child instances (e.g. `ProjectileInstance` LOB landing spawns a `ZoneInstance` "cloud") via `ctx.combatSystem.spawn(...)`. `BattleContext.combatSystem` is set after construction precisely so this back-edge exists.
- One-shot visuals go into `ctx.vfx` (drawn by `PlayState` after entities); persistent visuals stay inside the instance and draw via `CombatSystem.render`.
- `CombatSystem.update` iterates over a snapshot index so an instance that spawns another mid-tick doesn't get double-ticked the same frame.

### Combat & status pipeline
The `systems/combat/` package is the backbone for HP changes and reactive effects. It is intentionally isolated from `entities/` and `skills/` so it can be tested with fakes.

- **`DamageSystem.apply(DamageEvent)`** is the single entry point for HP loss. Order: (1) early-out if target is null/dead or amount ≤ 0; (2) defense mitigation — `max(1, raw * 100 / (100 + defense))`; (3) `bus.fire(ON_DAMAGE_TAKEN_PRE)` — shields/parries zero `ev.amount` here; (4) `Health.damage`; (5) `bus.fire(ON_DAMAGE_TAKEN)`, then `ON_HIT_LAND` for the attacker; (6) if HP reached 0, fire `ON_DEATH`/`ON_KILL` and call `target.onDeath()` (Enemy uses this to arm its 0.5s death fade).
- **`DamageSystem.heal(HealEvent)`** is the parallel path: write through `Health.heal`, fire `ON_HEAL`. Capped at `Health.max`.
- **`TriggerBus`** is a small pub/sub keyed on the `Trigger` enum. Listeners may filter to a specific `Combatant` so per-entity status effects (e.g. ShieldStatus on the *player*) ignore events on other targets. Dispatch snapshots the bucket so listeners can unsubscribe mid-fire.
- **`Triggers`**: `ON_HIT_LAND`, `ON_DAMAGE_TAKEN_PRE`, `ON_DAMAGE_TAKEN`, `ON_HEAL`, `ON_DEATH`, `ON_KILL`, `ON_TICK`. `TriggerEvent.combatant` is the *subject* of the trigger (attacker for `ON_HIT_LAND/ON_KILL`; target for the rest); `payload` is `DamageEvent` / `HealEvent` / `null` depending on trigger — listeners cast based on what they subscribed to.
- **`StatusContainer`** owns an `EnumMap<StatusType, Status>` per `Combatant`. `apply(status, bus)` either inserts (calling `onApply`, which is where reactive statuses subscribe to the bus) or refreshes the existing status's duration. `update(delta, dmg, bus)` snapshots the value set, ticks each, then removes any that expired. `has(StatusType)` is the read used by render code (freeze tint) and the AuraInstance fade check. `blocksMovement/Attack/Casting` are OR-folded over all live statuses and gate input + AI.
- **`Status`** base provides `remaining` countdown, optional `tickInterval` for periodic effects, and `blocksMovement/Attack/Casting` hooks. Concrete statuses:
  - `FreezeStatus` — blocks all three (movement/attack/casting); rendered as a blue sprite tint.
  - `StunStatus` — blocks movement + attack (casting is allowed).
  - `BurnStatus` / `PoisonStatus` — DoT via `dmg.apply(new DamageEvent(null, owner, dmgPerTick, STATUS, null))` every 0.5s (burn) / 1.0s (poison).
  - `RegenStatus` — HoT via `dmg.heal(new HealEvent(owner, healPerTick))` every 0.5s.
  - `ShieldStatus` — subscribes to `ON_DAMAGE_TAKEN_PRE` for its owner in `onApply`; the listener zeros `ev.amount` and self-expires (`remaining = 0`) so the next status tick fires `onExpire` (which unsubscribes). One incoming hit → shield consumed; otherwise expires by duration.
- **`StatusFactory.create(type, duration, dotMag)`** is the `Skill` → `Status` bridge. `dotMag` is the per-tick value (damage for Burn/Poison, heal for Regen, ignored for Freeze/Stun/Shield).
- **`Effect`** records (`DAMAGE`/`HEAL`/`APPLY_STATUS`/`KNOCKBACK`) are pure data on a `Skill`. `APPLY_STATUS` carries `statusType` + `duration` + `chance` + `value` (which becomes `dotMag` for DoT/HoT statuses).

### Input pipeline
- `InputSource` is the unified pollable interface (`isActionPressed`, `isActionJustPressed`, `update`).
- `KeyboardInputSource` keeps no internal state: it forwards both queries to `Gdx.input.isKey{Pressed,JustPressed}` and lets libGDX manage edges.
- `GamepadInputSource` *does* keep its own current/previous maps because gdx-controllers is event-driven (`buttonDown`/`buttonUp`/`axisMoved` callbacks). Trigger axes are thresholded (`> 0.5`) into booleans because L2/R2 arrive as analog values.
- `InputManager` composes any number of sources, ORs their pressed state per `GameAction`, and runs *its own* edge detection on the OR-folded result. This is the layer that prevents double-fires when both keyboard and pad are pushing the same action.

### Resource ownership & disposal
GPU resources have **three central owners**, loaded once and disposed once; everything else *borrows* and never disposes what it borrows:

- **`GameAssets` (package `assets/`, owned by `Main`)** wraps a single libGDX `AssetManager` and owns every file-loaded texture + audio clip: cave wall/floor, the clash VFX, the overworld avatar, the drop SFX + music, **and all monster directional sprites** (both `_se`/`_sw` facings for every `Monster`). `Main.create()` runs `assets.queueLoad()` then `assets.finishLoading()` (synchronous — `get()` is two-phase, never lazy, so the roster must be known up front; structured so an async loading screen could replace `finishLoading()` later). A typed facade (`caveWall()`, `clash()`, `avatar()`, `texture(path)`, …) hands shared instances to callers. `assets.dispose()` frees the whole manager at once.
- **`GeneratedAssets` (package `assets/`, owned by `Main`)** owns the runtime-generated (Pixmap-built) textures the `AssetManager` can't manage: one shared 1×1 white `pixel()` (HUDs draw it tinted for bars/fills) and the soft `shadow()` ellipse drawn under combatants. Built in `create()` (needs a GL context); `dispose()` frees both.
- **`SkillLibrary` (owned by `GameSession`)** owns each skill's icon, VFX texture, and any VFX animation sheet; all disposed in `SkillLibrary.dispose()` via `GameSession.dispose()`.

These three sets are **disjoint** — no texture is owned twice, so there is no double-dispose. Borrowers:
- **`MonsterRegistry` (package `registry/`, owned by `Main`)** owns nothing — it's a fetch facade. `getMonsterTexture(monster, team)` resolves the facing via `Monster.texturePath(team)` (player → east/`_se`, enemy → west/`_sw`) and pulls the texture from `GameAssets`. `Monster.texturePath` is the single source of truth for the sprite path: `GameAssets.queueLoad` loads with it, the registry fetches with it. `PlayState` wraps each fetched texture in a fresh `Sprite` per entity (a `Sprite` carries per-instance transform/color, so combatants can't share one).
- **`GameEnvironment`** (owned by `Main`, shared by `OverworldScreen` and `PlayState`) borrows the cave wall/floor textures (passed into its constructor from `GameAssets`); it owns only its `ModelBatch` and the `Model`s it builds.
- **`VfxManager` + `BloomEffect`** are owned by `Main` and shared across screens — both `PlayState` and `OverworldScreen` wrap their render in `vfxManager.beginInputCapture()` / `endInputCapture()` / `applyEffects()` / `renderToScreen()` so the same bloom pass runs everywhere. `Main.resize` forwards window resizes to `vfxManager.resize`.
- **`PlayState`** borrows `GameEnvironment`, `VfxManager`, and `BloomEffect` from `Main`, and constructs the per-battle `TriggerBus`/`DamageSystem` (plain Java, GC'd with the state). Its combatant sprites, clash, and shadow are all borrowed (`GameAssets`/`GeneratedAssets`). `PlayState.dispose()` only clears the per-battle `BattlefieldDecor` it added to the shared environment.
- **HUDs and `SkillSelectOverlay`** borrow the shared `GeneratedAssets.pixel()` (constructor-injected) — they own no textures and have no `dispose()`. `Battlefield` and `StatusContainer`s likewise hold no GPU resources.
- **`Main.dispose()`** frees `session` (→ `SkillLibrary`), `batch`, `font`, the active `screen`, then `generated`, `bloomEffect`, `vfxManager`, `environment`, and `assets`. `GameScreen.dispose()` only cascades into `playState` + `skillSelectState`.
- **Cross-screen disposal is centralized in `Main.setScreen`.** The override captures the outgoing screen, calls `super.setScreen(...)` (which fires `hide` on the old and `show`/`resize` on the new), then schedules `old.dispose()` via `Gdx.app.postRunnable`. Deferring to the next frame avoids the mid-render hazard where a screen's own `render()` call stack is still unwinding when `setScreen` is invoked. Individual screens therefore do **not** self-dispose after `setScreen`.

**Don't add `new Texture(...)` calls inside per-frame paths, and don't load files outside `GameAssets`.** File textures go through `GameAssets.queueLoad`; procedural textures live in `GeneratedAssets`. The shadow is the canonical procedural example — built once in `GeneratedAssets` and reused every frame.
## Core Classes
### `Main` (extends `Game`)
Application root. Owns shared rendering resources (`SpriteBatch batch`, `BitmapFont font`, `FitViewport viewport` — 16×9) plus the central asset layer: `GameAssets` (the single `AssetManager` wrapper — `create()` runs `queueLoad()` + `finishLoading()`), `GeneratedAssets` (shared `pixel` + `shadow`, built once GL is up), and `MonsterRegistry` (sprite fetch facade over `GameAssets`). `create()` pushes `MainMenuScreen`. Overrides `setScreen(Screen)` to dispose the predecessor via `Gdx.app.postRunnable` — see "Resource ownership & disposal" above.
### Screens
- **`MainMenuScreen`** — splash; switches to `GameScreen` on touch.
- **`GameScreen`** — owns the global per-battle objects (`SkillLibrary skills`, `ChargeMeter charge`) and HUDs (`ChargeBarHud`, `SlotsHud`, `BasicAttackHud`, `LifeBarHud`, `FpsHud`). Per-caster state (`SkillDeck`, `SkillSlots`, the basic-attack holder) lives on the `Caster`, not here. Constructor seeds `player.getDeck()` with every library skill *except* `wind_slash` — that one is assigned to `player.getCaster().setBasicAttack(...)` and fired via the dedicated `ATTACK_BASIC` button, so it deliberately stays out of the staging hand. Delegates per-frame `input/update/render` to whichever `GameScreenState` is active; the HUD pass draws on top of the state's rendering.
- **`GameOverScreen`** — post-battle "YOU WON" / "YOU LOST" overlay shown on a solid black backdrop. Constructed with a `Result { WON, LOST }`. Owns its own `InputManager` (default keyboard + gamepad). Restarts on `SKILL_SELECT_CONFIRM` (Enter or A) by calling `Main.setScreen(new GameScreen(...))`; the override disposes the spent `GameOverScreen` next frame.
### States
- **`PlayState`** — real-time combat. Reads movement (WASD), basic attack (J), slot fire (1/2/3), and the `Q+E` trigger combo to open the staging menu (gated on `charge.isFull()`). Owns `BattleContext` and `CombatSystem`, constructs the per-battle `TriggerBus` + `DamageSystem` and hands them to the context, and holds the `List<Enemy>` of all enemies on the field (currently seeded with two at `(COLS-2, 1)` and `(COLS-1, 2)` with `Stats(20,10,100,10,20)` each; the count is data-only and skill code does not care). Slot pops go through `player.getSlots()`; `onUsed` goes through `player.getDeck()`; both `handleAttack` (player basic attack) and `enemyAi` (each enemy's basic attack, looped) fire `SkillFactory.create(skill, casterCombatant)` through the same `CombatSystem.spawn` path. Player input checks `player.isInputLocked()` *and* `player.getStatusContainer().blocksMovement()` so stun/freeze gate firing as well as moving. Per-caster cooldowns tick inside `<entity>.update → caster.update → deck.update` (`tickMeters` only handles the global `ChargeMeter`). Each frame the order is `tickEntities → tickMeters → enemyAi → combatSystem.tickStatuses → combatSystem.update → checkBattleOver → tickAndCullEffects`. `checkBattleOver` flips to `GameOverScreen(WON)` when `allEnemiesDead()` (every entry in the list past its 0.5s death fade) and to `GameOverScreen(LOST)` on `!player.isAlive()`; a `transitionScheduled` flag prevents repeated transitions during the one-frame gap before disposal fires.
- **`SkillSelectState`** — staging menu. `enter()` snapshots `player.getSlots()`, draws a hand of 6 via `player.getDeck().drawHand(HAND_SIZE, player.getSlots())` (filtered by deck cooldowns + slots already loaded). Cursor navigation is `MOVE_LEFT/RIGHT`; `SKILL_X/Y/B` assign the highlighted card to that slot; `Enter` confirms (drains charge); `Esc`/`Back` cancels (slots restored from snapshot, charge preserved). Renders the world frozen behind the overlay.
### Entities
- **`Battlefield`** — `COLS=8 × ROWS=4` grid of `PanelType` enum values. Cols 0–3 are player territory, cols 4–7 enemy. Panel types: `NORMAL_BLUE`, `NORMAL_RED`, `CRACKED`, `BROKEN`, `ICE`, `LAVA`, `GRASS`, `POISON`. `PanelGenerator` has two factories: `generatePanels()` (default flat blue/red split, used today) and `generateMixedPanels()` (sprinkles hazard tiles — useful for testing once panel effects are wired).
- **`Player`** — `implements Combatant`. Composes `Caster(Team.PLAYER)`, `GridPosition`, `Health` (max HP = `Stats.vitality`, 100 today), `Stats`, and a `StatusContainer`. Half-grid clamped on `moveLeft/Right` (`col ∈ [0, COLS/2 - 1]`). Movement methods early-out if `caster.getInputLock().isLocked()` OR `statusContainer.blocksMovement()` — so freeze/stun gate keyboard movement uniformly with skill locks. `getCol/Row/VisualX/Y/DepthScale` delegate to `GridPosition`; `getTeam/InputLock/Deck/Slots/BasicAttack` delegate to `Caster`; `getHp/getMaxHp` read through `Health`. `HitFlash` provides the 300ms post-hit invisible-frame flicker. `update(delta)` ticks `caster.update` + `gridPosition.update` + `hitFlash.tick`. `forceSetTile(col,row)` writes via `gridPosition.setTile` (clamped only to grid bounds, not to the player half) so `StrikeInstance` can drive the body into enemy territory mid-skill. `Combatant` callbacks: `onHitFlash()` triggers the flicker; `onDeath()` is currently a no-op (no death fade yet — see Known Gaps). HP mutation happens exclusively through `DamageSystem`. **No bespoke basic-attack code** — pressing `ATTACK_BASIC` fires the `Skill` held in `getBasicAttack()` through the standard `SkillFactory` + `CombatSystem` pipeline (the same one slot fires use), gated by `getDeck()`'s cooldown map.
- **`Enemy`** — `implements Combatant`. Same composition (`Caster(Team.ENEMY)` + `GridPosition` + `Health` + `Stats` + `StatusContainer`), constrained to cols 4–7. Placeholder AI: `stepRandomly()` picks a random direction every 0.5–1.5s, skipped if `statusContainer.blocksMovement()`. `PlayState.enemyAi()` loops over every enemy and fires its held `basicAttack` through the unified pipeline every time deck cooldown + `wantsToBasicAttack()` (independent `attackTimer`/`attackInterval`) align — `wantsToBasicAttack` also early-outs on `statusContainer.blocksAttack()`. **Status & death are uniformly wired through the combat pipeline:** there is no bespoke `applyFreeze` anymore — `DamageSystem.apply` (or `Status.onApply`) feeds a `Status` into `StatusContainer`, and `render` checks `statusContainer.has(StatusType.FREEZE)` to apply the blue `FREEZE_TINT`. `Combatant.onDeath()` arms a 0.5s `deathTimer`; `isDying()` exposes the fade, `isDead()` reports when the fade has elapsed and the enemy can be treated as gone (render/shadow/AI all early-out). `render(batch, font)` also draws a small HP number centered just under the sprite (alpha fades with the death animation). Dead enemies stay in the list — they become inert, no spawner cost.
- **`Combatant`** (in `systems/combat/`) — interface implemented by both `Player` and `Enemy`. Exposes `Health`, `Stats`, `Caster`, `GridPosition`, `GridMovement`, `Team`, `StatusContainer`, the position shortcuts (`col/row/visualX/Y/depthScale`), `isAlive/isDead/isInputLocked`, and the lifecycle callbacks `onHitFlash()` + `onDeath()` the damage pipeline invokes. Every `SkillInstance`, every `Status`, and every `BattleContext` helper operates on `Combatant` so the same code targets either team.
- **`Team`** (in `components/`) — `PLAYER` / `ENEMY` enum read by `Caster`, by `ProjectileInstance` (for direction), and by `BattleContext.opposingOnRow` (filter targets by opposite team).
- **`BattleVfx`** (interface) / **`ClashEffect`** — `BattleVfx` is `update + render + isAlive`. `ClashEffect` scales 0.6→1.6 and fades over 0.25s; `PlayState` culls when `!isAlive()`.
### Skills
- **`Skill`** — pure, immutable data: `id`, `displayName`, `description`, `icon`, `Shape` (enum: `PROJECTILE/BEAM/STRIKE/AURA/ZONE`), `Element`, `List<Effect>` (unmodifiable), `cooldown`, **required** `vfxTexture`, optional `vfxAnimation` (`Animation<TextureRegion>` — when present, renderers prefer it over the static texture; this is how `ice_beam` animates) backed by an owned `vfxAnimationSheet` `Texture` so disposal is symmetric, optional `ShapeConfig`, and two damage-scaling knobs: `powerScale` and `magicScale`. When `applyEffectsTo` resolves a `DAMAGE` effect, the actual damage rolled into `DamageEvent.amount` is `effect.value + round(stats.power * powerScale) + round(stats.magic * magicScale)`. The same `Skill` can spawn many simultaneous `SkillInstance`s. Constructors are private — build with `Skill.builder()....build()`. The builder validates required fields and throws `IllegalStateException` naming the offending field on `build()`.
- **`ShapeConfig` / `ProjectileConfig`** — marker interface + per-shape tuning record. `ProjectileConfig` is the only concrete one today: `straight(speed)` for laser-style movement, `lob(targetRange, arcHeight)` for arcing tosses that land after `LOB_FLIGHT_TIME`. Add a new `ShapeConfig` impl when a shape grows tunable parameters.
- **`SkillInstance`** — abstract base for runtime executions. Tracks `combatant: Combatant` (firing entity), plus the convenience refs `caster: Caster` and `pos: GridPosition` pulled off the combatant. Snapshots origin tile (`originCol/originRow` from `pos` at construction), tracks `finished/lockTaken`. Methods: `acquireInputLock()` (via `caster.getInputLock()`), `releaseInputLock()`, `finish()`, abstract `update(delta, BattleContext)`, default no-op `render(batch, ctx)`. The single `applyEffectsTo(Combatant target, BattleContext ctx)` walks the skill's effects and routes through `ctx.damageSystem.apply` (DAMAGE — mixes in `powerScale`/`magicScale`), `.heal` (HEAL), or `target.getStatusContainer().apply(StatusFactory.create(...), ctx.triggerBus)` (APPLY_STATUS — rolls chance first). KNOCKBACK is parsed but currently a no-op stub.
- **`SkillFactory.create(Skill, Combatant)`** — single static switch on `Shape` returning the right `SkillInstance` subclass. Adding a new shape means adding an enum entry, a switch case, and a new `SkillInstance` subclass. Single `Combatant` arg — works for either Player or Enemy as the caster.
- **`SkillLibrary`** — global catalogue of every `Skill` the game knows about, and the owner of their GPU resources (icons, VFX textures, VFX animation sheets — all disposed by `SkillLibrary.dispose()`). `defaults()` loads from `assets/skills/skills.json` via `SkillLoader.load(...)`. The starter pool today is **nine** skills:
    - `wind_strike` — STRIKE, 30 base dmg + `powerScale 0.5`, 2.0s cd
    - `fire_blast` — PROJECTILE/straight (8 u/s), 20 dmg, 3.0s cd
    - `venom_bomb` — PROJECTILE/lob (range 2, arc 2.0), 12 dmg + spawns a `ZoneInstance` "cloud" on landing, 3.5s cd
    - `ice_beam` — BEAM, 25 dmg **+ FREEZE 2.0s @ 100% chance**, 4.0s cd; uses an `Animation<TextureRegion>` sliced from `skills/animations/icebeam_spritesheet.png` (0.1s frame time)
    - `frost_trap` — ZONE/Ice, 5 dmg per tick + FREEZE 3.0s @ 100%, 3.5s cd
    - `heal` — AURA, 15 HP instant heal on entering ACTIVE, 5.0s cd
    - `regen` — AURA, applies REGEN status (6 HP every 0.5s for 4.0s) on entering ACTIVE, 5.0s cd
    - `shield` — AURA, applies SHIELD status (blocks the next incoming hit; expires after 6.0s if unused), 4.0s cd
    - `wind_slash` — PROJECTILE/straight (12 u/s), 5 dmg, 0.5s cd, element NONE. Held in `Caster.basicAttack` by default; **not** added to the player's deck, so it never appears in `drawHand`.
  The library does **not** hand-draw — that's the deck's job. `GameScreen` seeds the player's deck with every library entry except `wind_slash` at battle start, and assigns `wind_slash` to every combatant's `basicAttack` slot.
- **`SkillDeck`** — per-caster standalone container of `Skill` references plus a private `SkillCooldowns`. Owned by a `Caster` (see Components). Methods:
    - `add(Skill)` / `remove(Skill)` / `contains(Skill)` / `all()` — membership; backed by a `LinkedHashSet<Skill>` so iteration order is stable for future inventory UIs (`drawHand` shuffles anyway).
    - `update(delta)` / `onUsed(Skill)` / `isOnCooldown(Skill)` / `remainingFor(Skill)` / `clearCooldowns()` — thin facade over the private `SkillCooldowns`.
    - `available()` — deck skills not currently on cooldown.
    - `drawHand(n, SkillSlots slots)` — eligible (off-cooldown + not in `slots`), shuffled, sliced to `n`. The deck has **no** back-reference to its caster; callers pass `SkillSlots` explicitly.
- **`SkillLoader`** — parses `skills.json` (top-level array) via libGDX's `JsonReader`/`JsonValue`. Each entry maps directly onto `Skill.builder()` setters; `effects` is a polymorphic list discriminated by `type` (`DAMAGE`/`HEAL`/`APPLY_STATUS`/`KNOCKBACK` — matching `EffectType`). `APPLY_STATUS` entries carry `statusType` (matching `StatusType`), `duration`, optional `chance` (default 100), and optional `value` (default 0, becomes the `dotMag` for DoT/HoT statuses). `shapeConfig.movementType` (`STRAIGHT`/`LOB`) dispatches to the existing `ProjectileConfig.straight(...)`/`.lob(...)` factories; `vfxAnimation` carries `spritesheet`, `frameWidth`, `frameHeight`, `frameDuration`; `powerScale`/`magicScale` are optional (default 0). Missing/malformed fields throw `IllegalStateException` naming the skill id and field path — no silent skipping.
- **`SkillCooldowns`** — `Map<String, Float>` remaining time. `update`, `onUsed`, `isOnCooldown`, `remainingFor`, `clear`. Lives inside a `SkillDeck` (one per caster); not exposed directly anywhere else. Ticked via `<entity>.update → caster.update → deck.update`.
- **`ChargeMeter`** — fills over time (max=1, fillRate=0.20/s ≈ 5s). `isFull()` gates the staging menu; `consume()` drains on confirm; `add(amount)` for future "charge on kill / on hit" rewards.
- **`StrikeInstance`** — Phases: `DASH_FORWARD` (0.10s) → `HIT` (0.20s) → `DASH_BACK` (0.10s) → `DONE`. Acquires the caster's `InputLock`, snaps to `originCol+1` via `pos.setTile(...)` (unclamped), spawns a `ClashEffect` on the target tile (`originCol+2`), looks up the target via `ctx.combatantAt(targetCol, row)` and applies effects if it isn't the caster's own team, then snaps back. The VFX anchors to the *tile* (`ctx.projectedTileWorld` + `ctx.tileDepthScale`) so the flourish still lands even when no one's there to take it. Single-target tile lookup; works for either team but the move-into-front-row logic still assumes player-style geometry (see Known Gaps).
- **`ProjectileInstance`** — Two movement modes via `ProjectileConfig`. **Team-aware**: `dir = caster.getTeam() == PLAYER ? +1 : -1`, applied to spawn-offset, velocity sign, grid-exit edge, sprite horizontal flip, and target lookup.
    - `STRAIGHT`: spawns at the caster's visual position and travels `dir * config.speed`; finishes when it crosses the appropriate grid edge or **overlaps the first opposing combatant's tile center on the same row**. Iterates `ctx.opposingOnRow(combatant, row)` and stops on the first within `halfW` — a closer target soaks the hit so anyone behind survives. Same code handles either team because the lookup is team-relative.
    - `LOB`: parabolic arc from caster to `originCol + targetRange * dir` over `LOB_FLIGHT_TIME` (0.50s); on landing applies damage via `ctx.combatantAt(landCol, row)` (skipping same-team targets) — only the combatant standing on the landing tile takes the hit. The follow-up `ZoneInstance` "cloud" is then spawned via `ctx.combatSystem.spawn(...)` for lingering effect (tile-pinned, so subsequent ticks affect whichever target walks into the cloud). LOB-from-enemy is latent (no enemy fires LOB skills today).
  Does **not** acquire `InputLock` — the caster keeps playing while their projectile flies. Sprite size is resolved lazily from `ctx.battlefield` on the first update tick (no dependence on the caster's own sprite).
- **`BeamInstance`** — Phases: `CHARGE` (0.20s) → `FIRE` (0.70s) → `FADE` (0.25s) → `DONE`. Acquires `InputLock` for the windup; releases at the start of `FADE`. On entering `FIRE` it **pierces every opposing combatant** on the caster's row past `originCol` via `ctx.opposingOnRow(combatant, row)` (one application each, guarded by the `hitApplied` flag so the loop runs exactly once per fire). Render: if `Skill.vfxAnimation` is present (e.g. `ice_beam`'s sprite-sheet) the beam draws the current `Animation` frame; otherwise it stretches the static `vfxTexture`. During `CHARGE` the beam extends out from the caster (`w = fullW * (phaseTime/CHARGE)`); during `FADE` alpha lerps to zero.
- **`AuraInstance`** — Phases: `EXPAND` (0.20s) → `ACTIVE` (variable) → `FADE` (0.20s) → `DONE`. **Self-targeted**: on entering `ACTIVE` it calls `applyEffectsTo(combatant, ctx)` once — instant `HEAL`s apply immediately (`heal`), `APPLY_STATUS` effects attach a Status to the caster's container (`regen`, `shield`). The ACTIVE duration is data-driven: if the skill has any `APPLY_STATUS` effects, ACTIVE lasts as long as the longest status duration; otherwise it falls back to a default 3.0s. `shouldFade()` also returns true early when every status the skill applied has been removed (e.g. a Shield consumed by an incoming hit immediately fades the aura visual). Renders a scaled, pulsing sprite on the caster's tile. Fires `Trigger.ON_TICK` once on entering ACTIVE — the per-tick HoT/DoT cadence then lives inside the Status itself, not in the AuraInstance.
- **`ZoneInstance`** — Phases: `APPEAR` (0.15s) → `ACTIVE` (1.00s) → `FADE` (0.25s) → `DONE`. Pinned to a target tile (defaults to `caster +1, sameRow`; can be overridden — that's how `ProjectileInstance` plants a cloud on its landing tile). Every `TICK_INTERVAL` (0.33s) it resolves the target via `ctx.combatantAt(targetCol, targetRow)`, skips same-team, and applies effects — so a target walking off the tile escapes subsequent ticks, and one walking on starts taking them. Returns `true` from `isRenderUnder()`, so it draws under entities (terrain feel).
- **`slots/`** — `SlotKey` (`X, Y, B`), `ButtonSlot` (FIFO `ArrayDeque<Skill>`, `CAPACITY=2`), `SkillSlots` (`EnumMap<SlotKey, ButtonSlot>` + `contains(Skill)`). A `SkillSlots` instance lives on each `Caster` (accessed via `caster.getSlots()`) — not on `GameScreen`. Slots persist across `PlayState` ↔ `SkillSelectState` transitions; cancel restores from a snapshot taken on `enter()`.
- **`effects/Effect`** — immutable record with type, value, duration, chance, and statusType fields. Factory constructors: `damage(int)`, `heal(int)`, `applyStatus(StatusType, duration, chance, dotMag)`, `knockback(tiles)`. `SkillInstance.applyEffectsTo(Combatant, BattleContext)` walks the effect list and routes each entry through `DamageSystem` (DAMAGE — mixes in `powerScale*power + magicScale*magic`), `DamageSystem.heal` (HEAL), or `target.getStatusContainer().apply` (APPLY_STATUS — rolls chance first; status is built by `StatusFactory.create`). KNOCKBACK is parsed and stored but is a stub today.
- **`effects/EffectType`** — `DAMAGE, HEAL, APPLY_STATUS, KNOCKBACK`. Status-specific types (FREEZE/BURN/POISON/STUN/REGEN/SHIELD) live in `systems/combat/StatusType` and are carried inside an `APPLY_STATUS` effect via the `statusType` field.
- **`elements/Element`** — `NONE, FIRE, POISON, ICE, LIGHTNING`. Currently cosmetic — no element-vs-element resistance/weakness logic yet.
### Systems
- **`BattleContext`** — bag of references constructed once in `PlayState`: `battlefield`, `player`, `List<Enemy> enemies`, `List<BattleVfx> vfx`, `GameEnvironment environment`, the shared `clashTexture` (`effects/clash.png`, used by `CombatSystem.resolveProjectileClashes` to spawn the cancel flourish), the per-battle `damageSystem` and `triggerBus`, *and* a back-reference to its owning `combatSystem` set immediately after construction. Exposes four target-lookup helpers used by every skill instance:
    - `enemyAt(col, row)` — first alive `Enemy` on that exact tile, or `null` (legacy/internal).
    - `enemiesOnRow(row)` — fresh list of alive `Enemy`s on a row (legacy/internal).
    - `combatantAt(col, row)` — checks the player first, then enemies; returns the first alive `Combatant` on that tile (or `null`). The team-aware lookup Strike/Zone/Projectile-LOB use.
    - `opposingOnRow(attacker, row)` — fresh list of alive combatants on the row whose team differs from `attacker`. Used by Beam (pierce all) and Projectile straight (stop on first overlap). Works for either team because the filter is "opposite of attacker."
  Projection helpers `projectedTileWorld(col,row)` (tile center in 2D viewport space) and `tileDepthScale(row)` (1.0 near → ~0.78 far) are how skill instances place their VFX correctly on the 3D-projected floor. `buildCache()` (called from `PlayState.resize`) bakes a `COLS×ROWS` lookup so every per-frame skill draw is a constant-time array hit, not a `cam3D.project()` call. Pass-around context (not a service locator) so skills can be unit-tested with fakes.
- **`CombatSystem`** — owns `List<SkillInstance> active`.
    - `spawn(instance)` adds to the active list (callable from anywhere with a `BattleContext`, including from inside another `SkillInstance.update`).
    - `tickStatuses(delta)` walks the player and all enemies and ticks each alive combatant's `StatusContainer` against the shared `damageSystem` + `triggerBus`. Run *before* `update(delta)` in the per-frame loop so DoT kills and buff expirations land before this frame's skill instances resolve.
    - `update(delta)` ticks every active instance over a snapshot index — instances spawned mid-tick join the list but won't be ticked until next frame. Between the per-instance update and the finished-cull, `resolveProjectileClashes()` walks `active` pairwise: opposing-team STRAIGHT `ProjectileInstance`s on the same row whose centres are within one (depth-scaled) panel-half-width spawn a single `ClashEffect` at the midpoint and both `finish()`. LOBs are excluded — they're parabolic, not piercing.
    - `renderUnder(batch)` draws zone instances flagged `isRenderUnder()`; `render(batch)` draws everything else. Two-pass rendering is what lets zone tiles sit visually beneath player/enemy sprites.
    - `hasActive()` for future menu/pause logic.
- **`GameEnvironment`** — the 3D scene that hosts the battlefield (currently dressed as a cave; the class itself is theme-agnostic). Owns a `PerspectiveCamera` (45° FOV, positioned high and tilted ~40° below horizontal), an `Environment` with ambient + directional + four point lights tinted to match the crystal palette, and a `ModelBatch` driving `ModelInstance`s built once in `buildGeometry()` (floor slab, back wall, ceiling, side walls, eight stalactites, and the COLS×ROWS panel grid — blue diffuse + blue-specular on the player side, red on the enemy side, so the point lights cast tinted glows on each tile). Exposes two helpers consumed by `BattleContext`: `projectTile(col,row)` maps a tile center on the 3D floor into 2D viewport world coordinates so sprites can be drawn standing on it, and `tileDepthScale(row)` returns a linear scale `1.0 → 1.0 - DEPTH_SCALE_FAR (0.22)` from near to far row. `dispose()` releases all built `Model`s plus the wall/floor textures and the `ModelBatch`.

### Combat (systems/combat)
- **`Combatant`** — described above under Entities. Player and Enemy both implement it.
- **`DamageSystem`** — single entry point for HP mutation. `apply(DamageEvent)`: defense mitigation → `ON_DAMAGE_TAKEN_PRE` (shields zero damage here) → `Health.damage` + `onHitFlash` → `ON_DAMAGE_TAKEN`/`ON_HIT_LAND` → on death, `ON_DEATH`/`ON_KILL` + `target.onDeath()`. `heal(HealEvent)`: `Health.heal` (capped at max) + `ON_HEAL`. No other code path mutates HP.
- **`TriggerBus`** — pub/sub keyed on the `Trigger` enum. `subscribe(trigger, combatant filter, listener)` returns a `Subscription` for cleanup; passing a non-null filter restricts the listener to events whose `combatant` matches. `fire(TriggerEvent)` snapshots the bucket before dispatch so listeners may unsubscribe mid-fire.
- **`Trigger`** — `ON_HIT_LAND`, `ON_DAMAGE_TAKEN_PRE`, `ON_DAMAGE_TAKEN`, `ON_HEAL`, `ON_DEATH`, `ON_KILL`, `ON_TICK`. Payload type is documented on `TriggerEvent` and is part of the contract, not the type system (events are `Object`).
- **`Status` / `StatusContainer` / `StatusFactory`** — described in detail under "Combat & status pipeline." `Status` is the base; six concrete subclasses live under `systems/combat/status/`:
    - `FreezeStatus` — blocks movement + attack + casting; rendered as blue sprite tint via `StatusContainer.has(FREEZE)`.
    - `StunStatus` — blocks movement + attack only.
    - `BurnStatus(dmg)` — 0.5s tick DoT.
    - `PoisonStatus(dmg)` — 1.0s tick DoT.
    - `RegenStatus(heal)` — 0.5s tick HoT.
    - `ShieldStatus` — subscribes to its owner's `ON_DAMAGE_TAKEN_PRE` in `onApply`; zeros the next incoming `ev.amount` and self-expires; `onExpire` unsubscribes.
- **`event/DamageEvent`** — mutable carrier (`source`, `target`, `amount`, `sourceTag: SKILL/STATUS/PANEL/UNKNOWN`, `originalSkill`). `amount` is intentionally mutable so `ON_DAMAGE_TAKEN_PRE` listeners (Shield, future parries) can rewrite it.
- **`event/HealEvent`** — `target` + mutable `amount`. Symmetric to DamageEvent for over-heal cap or future buff math.
- **`event/TriggerEvent`** — wrapper passed to listeners: `trigger`, `combatant` (the subject — attacker for `ON_HIT_LAND/KILL`, target otherwise), `payload` (cast to the right event type per trigger).
### Components
The `components/` package is for ECS-style role components — entities compose them to opt into behaviors. Unlike leaf helpers in `util/`, components in this package may freely depend on `skills`.
- **`Caster`** — role component for entities that can stage and cast skills. Owns:
    - a final `Team team` (set in ctor; read by team-aware skill instances and exposed via `getTeam()`),
    - a final `SkillDeck deck` (skills the caster has access to + their cooldown timers),
    - a final `SkillSlots slots` (the X/Y/B staged loadout),
    - a final `InputLock inputLock` (held by skill instances during dash/cast — gates both entity movement and further skill firing),
    - a mutable `Skill basicAttack` (the skill fired by the dedicated `ATTACK_BASIC` button — swappable at runtime via `setBasicAttack(Skill)`).
  Exposes `getTeam()` / `getDeck()` / `getSlots()` / `getInputLock()` / `getBasicAttack()` / `setBasicAttack(Skill)` and an `update(delta)` that ticks `deck.update(delta)`. Composed onto `Player` and `Enemy`; the entity's own `update` must call `caster.update(delta)` so cooldowns tick. The entity also delegates these getters directly so call sites stay clean (`player.getBasicAttack()` rather than `player.getCaster().getBasicAttack()`).
- **`GridPosition`** — position component for entities living on the panel grid. Owns `col`, `row`, a `PositionSmoother` (world-space tween between tile centers), `projectedTargetX/Y` (the screen-projected target `PlayState.tickEntities` pushes in each frame), and `depthScale` (perspective scale for the row). `setTile(col, row)` is unclamped — the owning entity is responsible for movement rules (half-grid clamp for Player, enemy-territory clamp for Enemy). `update(delta)` smooths toward the projected target (or tile center, if no target yet). Composed onto `Player` and `Enemy`; the entity's own `update` calls `gridPosition.update(delta)`. `forceSetTile` on entities is just a thin wrapper around `gridPosition.setTile(...)` used by `StrikeInstance` to drive the entity across territory.
- **`Health`** — `current` and `max` HP. Two mutators: `damage(int)` clamps to 0, `heal(int)` clamps to `max`. **Mutated only inside `systems/combat/DamageSystem`** — every other code path reads via `getCurrent()` / `getMax()`. Entities expose `getHp()` / `getMaxHp()` shortcuts that delegate here. Max HP is seeded from `Stats.vitality` at entity construction.
- **`Stats`** — `power`, `magic`, `vitality`, `defense`, `speed` (ints). Immutable today; set once at construction. `power`/`magic` are mixed into damage rolls via `Skill.powerScale` / `magicScale`; `vitality` is the initial max HP; `defense` feeds `DamageSystem.applyDefense` (percent reduction `raw * 100 / (100 + defense)`, floor 1); `speed` is parsed but not yet consumed. PlayState seeds every combatant with `Stats(20, 10, 100, 10, 20)` today — see Known Gaps.
- **`Team`** — `PLAYER` / `ENEMY` enum. Lives here (not in `entities/`) so `Caster` can hold one without `components/` having to import `entities/`.

### Util (leaf helpers)
- **`InputLock`** — owner-based lock. `lock(owner)` refuses if a different owner holds it; `unlock(owner)` is a no-op if you're not the holder. Owner is `Object` (not `SkillInstance`) so `util` stays a leaf package and skill instances can hold the lock without `util` depending on `skills`. Identity comparison is all that matters.
- **`HitFlash`** — post-hit flicker timer (0.3s total, 0.05s flicker interval). `flash()` arms; `tick(delta)` decays; `isHidden()` returns true on alternating intervals while armed. `Player.render` and `Enemy.render` skip the draw when `isHidden()` is true, producing the invisible-frame strobe.
- **`PositionSmoother`** — exponential easing toward a target `(x, y)`. Used by `Player` and `Enemy` to tween between tile centers.
- **`PanelGenerator`** — static factories that produce the `PanelType[][]` grid handed to `Battlefield`. Two presets: `generatePanels()` (flat blue/red split, used today) and `generateMixedPanels()` (sprinkles hazard tiles).
### UI
- **`ChargeBarHud`** — small charge meter sitting just above the `SlotsHud` panels in the bottom-left, width-matched to the X/Y/B slot row so the two HUDs read as one stack. Batch-aware (`wasDrawing` check).
- **`SlotsHud`** — bottom-left X/Y/B columns with FIFO icons (top of column = front of queue), placeholder for empty cells.
- **`BasicAttackHud`** — bottom-left icon one slot-gap to the right of the X/Y/B column, showing the skill held in `player.getBasicAttack()`. Renders a translucent black "cooldown veil" over the top portion of the icon when on cooldown — the veil shrinks downward as `deck.remainingFor(skill)` ticks toward 0, the standard MOBA-HUD cooldown sweep. Borrows the shared 1×1 white pixel from `GeneratedAssets` (constructor-injected; not owned).
- **`LifeBarHud`** — horizontal player HP bar anchored to the top-left of the viewport. Fill ratio is `player.getHp() / player.getMaxHp()`; tint shifts green → yellow → red as HP drops. Borrows the shared 1×1 white pixel from `GeneratedAssets` (constructor-injected; not owned).
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
1. `PlayState.checkBattleOver` (called each frame after `combatSystem.update`) reads `allEnemiesDead()` (every entry in `enemies` has finished its `onDeath()` fade) and `!player.isAlive()`.
2. On win, each enemy's 0.5s death fade plays first (`isDying`) — the fade is armed by `Combatant.onDeath()` which `DamageSystem` fires when HP hits 0; after every enemy's `isDead()` flips true the win transition fires via `Main.setScreen(new GameOverScreen(WON))`.
3. On loss, transition is immediate — `Player.onDeath()` is a no-op stub, so there is no symmetric death fade yet (see Known Gaps).
4. `Main.setScreen` swaps screens and queues `GameScreen.dispose()` for the next frame via `postRunnable`.
5. `GameOverScreen` shows "YOU WON" / "YOU LOST" + the restart hint; pressing **Enter** or **A** calls `Main.setScreen(new GameScreen(...))`, which in turn disposes the spent `GameOverScreen` — yielding a fully fresh battle every restart.
## Design Rationale
- **Charge gates the menu, cooldowns gate the hand.** Two coexisting timers with different jobs: `ChargeMeter` controls when you can stage, per-skill cooldowns control which skills are *available* to stage or fire.
- **Slots persist across menu opens** (Battle Network "custom screen" feel).
- **Snapshot/restore on cancel** rather than journaling individual assigns. Simpler and player-indistinguishable.
- **`InputLock` owner is `Object`** so `util/` stays a leaf package — a `SkillInstance` can hold the lock without `util` depending on `skills`. Identity comparison is all that matters.
- **Per-caster state lives on `Caster`, not `GameScreen`.** `SkillDeck` (which owns its own `SkillCooldowns`) and `SkillSlots` are properties of *this* casting entity. Cooldowns are the player's, not the battle's; the same applies to the staged loadout. `GameScreen` keeps only what's truly battle-global: the skill catalogue, the charge meter, and the HUDs.
- **`SkillDeck` has no caster back-reference.** Membership + cooldowns are intrinsic to the deck, but anything that needs to filter by slots (like `drawHand`) takes them as a parameter — keeps `SkillDeck` a standalone data structure and avoids the `Caster ↔ SkillDeck` loop.
- **`MovementSystem` is the single owner of *all* position writes — grid and free.** Grid methods take a `Combatant`: `tryGridStep(Combatant, Direction)` (input/AI — honors input-lock + movement-blocking status, clamps to the entity's `GridBounds`), `forceGridTeleport(Combatant, col, row)` (skill dashes/teleports — clamps only to the global grid edge, which is how a Strike's HIT phase legally enters enemy territory), and `applyDisplacement(Combatant, tiles, Direction)` (the named home for knockback/pull). The free branch takes a `FreePosition`: `applyFreeInput(FreePosition, dx, dy, delta)` (overworld avatar — integrates by speed, clamps to room bounds). The *mode* lives in which method the scene calls, not in a flag — so the overworld is a localized addition, not a re-architecture. `GridPosition.setTile` / `FreePosition.set` stay public but are, by convention, called only from here.
- **VFX reuses `ClashEffect`** because it already does scale+fade over a short lifetime — exactly the slash flourish we want. Per-skill VFX textures live on `Skill.vfxTexture` and are required non-null — every skill must supply one.
- **Hit resolution lives inside each `SkillInstance`** (synchronous, fixed-timing). There's no separate collision system — every projectile, beam, strike, and zone does its own tile-center overlap test inside its `SkillInstance` subclass. Multi-target lookups go through `BattleContext.combatantAt` / `opposingOnRow` so the targeting semantics live in one place per shape rather than in a shared collision sweep.
- **HP only mutates through `DamageSystem`.** A single chokepoint means defense, shields, hit-flash, death-fade trigger, and the `ON_DAMAGE_TAKEN_PRE/ON_DAMAGE_TAKEN/ON_HIT_LAND/ON_DEATH/ON_KILL` fan-out are written once and impossible to bypass. The cost is paying one virtual call per hit; the win is that adding the next reactive effect (parry, lifesteal, on-kill heal) is a `TriggerBus.subscribe(...)` call inside a new `Status` and nothing else.
- **`Combatant` interface replaces concrete `Player`/`Enemy` references in the combat pipeline.** Every `SkillInstance`, every `Status`, and every `BattleContext` lookup operates on `Combatant`. The same Strike or Beam code therefore aims at either team based on `combatant.getTeam()`. This is the layer that made multi-enemy and (eventually) PvP-style enemy casting cheap to add.
- **Statuses subscribe to triggers in `onApply`, unsubscribe in `onExpire`.** Reactive effects (Shield, future parry/thorns/lifesteal) are written as a `Status` subclass whose `onApply` calls `bus.subscribe(Trigger.X, owner, listener)` — the owner-filter on the bus restricts the listener to that combatant, and the returned `Subscription` is held in the status for `onExpire` to clean up. No global handler registries; status lifecycle and reactive listener lifecycle are the same lifetime.
- **`SkillFactory` takes a `Combatant`, not bare components.** `SkillFactory.create(Skill, Combatant)` — the instance pulls `caster`/`pos` off the combatant, but every downstream effect application targets `Combatant`s through `BattleContext` helpers. Avoids the four-arg signature the old `(Skill, Caster, GridPosition)` form was sliding toward as new components arrived.
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
- **Overworld is a thin slice.** `OverworldScreen` gives free-roam movement in the bare cave and two door floor-rects that launch a battle, with `GameSession`/`PlayerProfile` persisting the deck across the round-trip. What's missing: overworld *props* (the floor-decoration counterpart to battle panels — `GameEnvironment` is ready for them via `addDecor`), doors as actual wall openings rather than floor zones, varied/connected rooms, a seamless (non-hard-swap) transition, HP/position persistence, and a real per-room encounter/spawn definition (today every door spawns the same hardcoded battle).
- **No entity animation system.** `Skill.vfxAnimation` covers animated VFX (used by `ice_beam`), but `Player`/`Enemy` themselves are still single static sprites. Pattern when ready: `AnimController` component holding `Map<State, Animation<TextureRegion>>` + current state + elapsed time, ticked by an `AnimSystem`. `Player.render()` would read the current frame; `StrikeInstance` would set `ATTACKING` on entering `HIT`, `NEUTRAL` on entering `DASH_BACK`.
- **Strike/Beam/Aura/Zone use team-aware target lookups but still bake player-side geometry.** Targets are looked up via `ctx.combatantAt` / `ctx.opposingOnRow` so the *who-gets-hit* question is correct for either team. What's not generalized: `StrikeInstance` snaps to `originCol+1` and aims at `originCol+2` regardless of team (an enemy-fired Strike would dash into its own territory rather than toward the player). When enemies need to fire those shapes, parameterize the "dash forward" direction the same way `ProjectileInstance.dir` already is.
- **`KNOCKBACK` effect is parsed but unimplemented.** `Effect.knockback(tiles)` builds the right record and `EffectType.KNOCKBACK` ships through the loader, but `applyEffectsTo`'s switch leaves it as a no-op. Implementation hook: one line in `applyEffectsTo`'s `KNOCKBACK` case — `ctx.movementSystem.applyDisplacement(target, tiles, awayFromCaster)`, where direction is derived from `caster.getTeam()`.
- **Stats are static and uniform.** PlayState constructs both the player and every enemy with `Stats(20, 10, 100, 10, 20)`. There's no level-up, gear, per-character stat block, or scaling system yet. The `Stats` object is plumbed everywhere it needs to be — what's missing is anything that *varies* it.
- **No I-frames during Strike.** Player can be hit while dashing forward. Simplest fix: a status (`I_FRAMES`?) or flag that `DamageSystem.apply` checks before mutating HP; cleanly composes with the existing `ON_DAMAGE_TAKEN_PRE` mechanism.
- **Enemy doesn't use staged skills yet.** It fires `wind_slash` via `Caster.basicAttack` through the unified pipeline, but its `SkillDeck` and `SkillSlots` are empty and no AI logic stages anything. When enemies need to cast staged skills: populate the deck (`enemy.getDeck().add(...)`) and write an AI that picks a skill, runs the same `deck.onUsed` + `SkillFactory.create(skill, enemy)` + `combatSystem.spawn` sequence `PlayState.enemyAi` already does for basic attack.
- **Enemy AI is a placeholder** — random walk + fixed-cooldown basic attack. No targeting, no skills, no panel awareness.
- **Player death is still abrupt.** Win/lose flow is wired (`PlayState.checkBattleOver` → `GameOverScreen`), `Combatant.onDeath()` is on the interface, and Enemy uses it to arm the 0.5s death fade — but `Player.onDeath()` is an empty stub, so dropping to 0 HP transitions immediately. A symmetric 0.5s fade hook would feel cleaner.
- **No panel state transitions** (`NORMAL → CRACKED → BROKEN` on stand/leave).
- **Panel effects** (ICE slide, LAVA/POISON DoT, GRASS heal) are defined as `PanelType`s and have textures, but no behavioral hooks yet. `DamageEvent.Source.PANEL` is plumbed through specifically so panel-DoT will fan through `DamageSystem` once wired.
- **Multi-enemy support landed; encounter design hasn't.** `PlayState` holds a `List<Enemy>` (seeded with two today). What's still missing is anything that *uses* it as a slot for variety: no spawner, no encounter definition, no varied enemy stats or sprites, no waves. The list count is hardcoded in `PlayState`'s constructor.
- **`SkillSelectOverlay` was tuned for 4 cards** but `HAND_SIZE = 6`; layout may overflow at 6. Worth a visual pass.
- **3D camera is fixed.** `GameEnvironment` poses the camera once at construction. No screen-shake hook yet — adding one means animating `cam3D.position`/`lookAt` and re-baking `BattleContext`'s projection cache (or invalidating it).
- **AuraInstance fires `ON_TICK` exactly once** (on entering ACTIVE). The inline comment `"might need move to status onTick??"` flags this as a design question — today periodic application is the Status's job (`RegenStatus.onTick` heals every 0.5s), but if an aura needs a per-tick effect that *isn't* a Status, the trigger fire would need to repeat. Decide before the next reactive-on-ON_TICK status lands.
- **Fusion system parked.** Earlier design explored shape+element fusion tables; explicitly out of scope until the existing shapes feel good. Once it's revisited, fusion becomes "given two `Skill`s + their `Shape`/`Element`, produce a third or modify a spawned `SkillInstance`."
- **Main menu is still text-only** — two `font.draw` lines with no art, no buttons, no controller support (tap-to-start only). A proper title screen with selectable entries is future work.
