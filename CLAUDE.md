# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Authoritative spec

`SPEC.md` at the repo root is the canonical, up-to-date design doc. Read it before making non-trivial changes — it covers the architecture, the input map, the skill flow, design rationale, and the known gaps. Keep `SPEC.md` in sync when you change architecture; the README is template boilerplate and is not maintained.

## Build & run

This is a Gradle multi-project build (`core`, `lwjgl3`) using the Gradle wrapper. Java 25 toolchain (auto-fetched via foojay-resolver). On Windows use `gradlew.bat`; on Unix `./gradlew`.

- `./gradlew lwjgl3:run` — launch the desktop game (working dir is `assets/`).
- `./gradlew lwjgl3:jar` — build runnable JAR at `lwjgl3/build/libs/evoforce-<version>.jar`. Variants: `jarWin`, `jarMac`, `jarLinux` (platform-specific, smaller).
- `./gradlew core:clean` / `./gradlew clean` — clear build outputs for one project / all.
- No tests yet — `./gradlew test` is a no-op.

`processResources` depends on `generateAssetList`, which writes `assets/assets.txt` (a recursive manifest of `assets/`) on every build.

## Architecture in one screen

- **libGDX game**, single `Main extends Game` with a 16×9 `FitViewport`. `Main` owns the shared `SpriteBatch`/`BitmapFont`; screens borrow them.
- **Top-level screens**: `Main` swaps `Screen`s — `MainMenuScreen` → `OverworldScreen` → `GameScreen` (battle), back to `OverworldScreen` on win or `GameOverScreen` on loss. `Main` owns a `GameSession` (the run): the `SkillLibrary` plus a persistent `PlayerProfile` (`Caster` + `Stats`) that survives across battles. (Both `GameSession`/`PlayerProfile` live in `sessions/`.)
- **Screen → State** layering (battle only): `GameScreen` runs a `GameScreenState` machine (`PlayState` ↔ `SkillSelectState`), owns the `ChargeMeter` + HUDs, and reads the run off `game.session`. Per-caster state (deck/slots/basic attack) lives on the `Caster`; the catalogue + player loadout live on `GameSession`, not `GameScreen`. The state machine is not a stack.
- **HD-2D rendering, one environment for both scenes.** `GameEnvironment` (in `environment/`) renders a 3D cave via `ModelBatch`; 2D sprites are billboards projected onto the floor. It's grid-agnostic: `SceneCamera` owns the camera + continuous `project(x,z)`/`depthScale(z)`; `Battlefield` owns the grid layout (`floorX`/`floorZ`); battle panels are floor decoration added via `BattlefieldDecor`. The overworld uses the same environment with no panels.
- **Pragmatic component-style, not ECS.** `Player`/`Enemy` are concrete classes composing small components (`Caster`, `GridMovement`, `Health`, `Stats`, `StatusContainer`, …). Cross-cutting behavior goes in `systems/` (`CombatSystem`, `MovementSystem`, `BattleContext`). Ashley is on the classpath but not used — don't reach for it.
- **Skill execution.** `Skill` is data; `SkillFactory.create(Skill, Player)` switches on `Shape` to produce a runtime `SkillInstance`. `CombatSystem` ticks active instances and prunes finished ones. Only `StrikeInstance` is fully wired (`wind_strike`); `Projectile/Beam/Aura/ZoneInstance` are stubs that finish on first tick to keep the factory exhaustive.
- **Two coexisting timers, distinct jobs.** `ChargeMeter` (≈20s fill) gates *opening the staging menu*; per-skill `SkillCooldowns` gates *which cards appear in the hand and which slots can fire*. Don't conflate them.
- **Slots persist across menu opens.** `SkillSelectState.enter()` snapshots `SkillSlots`; cancel restores the snapshot, confirm drains charge. Hand is filtered against both `cooldowns` and currently-loaded slots.
- **`InputLock` owner is `Object` by design** so the `components` package doesn't depend on `skills`. Identity comparison is the contract.
- **`MovementSystem` is the sole owner of entity position writes.** Input steps go through `tryGridStep` (honors input-lock, movement-blocking status, and per-entity `GridBounds`); skill-driven dashes/teleports go through `forceGridTeleport`, which clamps only to the global grid edge so a Strike's HIT phase can drive the player into enemy territory. Nothing else calls `GridPosition.setTile` directly — that's a convention, enforced by review. Free (non-grid) movement — the overworld avatar — goes through `applyFreeInput(FreePosition, …)`; the system is the sole position writer for both grid and free modes.

## Conventions worth knowing

- **Package root:** `com.silverignis` under `core/src/main/java/`. Launcher lives in `lwjgl3/...`.
- **World units:** the battlefield is 8 cols × 4 rows; cols 0–3 are player territory, 4–7 enemy. Use `BattleContext.tileWorldX/Y` for tile→world conversion rather than recomputing offsets.
- **Input dual-purpose by state.** `SKILL_X/Y/B` fire the front of a slot in `PlayState` and assign the highlighted hand card in `SkillSelectState`. Gamepad cancel is on **Back**, not B, so B doesn't double-fire (assign + cancel) inside the menu.
- **VFX shortcut.** `ClashEffect` is reused as the generic short-lived skill VFX (scale+fade over a short lifetime). Per-skill VFX texture support on `Skill` is a deliberate TODO, not an oversight.
- **No animation system.** Sprites are static PNGs; movement is tweened by `PositionSmoother`. If you need animation, see the pattern sketched in `SPEC.md` "Known Gaps" (`AnimController` + `AnimSystem`); don't introduce a one-off.

## Asset pipeline

Art is generated offline by Python/Pillow scripts under `tools/` (some hit the PixelLab API). Outputs live under `assets/{panels,sprites,attacks,effects,skills}/`. The scripts are run manually — they are not part of the Gradle build. The `generateAssetList` task is what wires `assets/` into the runtime classpath via `assets.txt`.
