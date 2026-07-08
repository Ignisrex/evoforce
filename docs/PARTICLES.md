# Particle System — Design & Handoff

Status: design agreed, not yet implemented.
Package root: `com.silverignis` (engine lives in `com.silverignis.particles`).

## 1. Purpose & scope

One particle system serving many uses — atmospheric ambience (Hollow Knight–style
drifting dust), skill VFX enrichment (bursts, embers, charge-up convergence, flares),
and later UI/overworld flourishes. The whole point of the structure below is that a
dust mote and a skill spark are the **same primitive**; they differ only in their
emission schedule, anchor, and feel — all of which is config, not new code.

Non-goals for v1: data-driven authoring (see §7), pixel-quantized particle look,
libGDX's built-in particle system (see §8).

## 2. Architecture — four levels

Responsibility drops as you go down. Each level knows nothing about the level above.

| Level | Type | Owns | Knows about gameplay? |
|-------|------|------|----------------------|
| Engine | `ParticleEngine` | pooled buffer, sim tick, budget, batched render submit, cleanup | no |
| Unit | `Particle` | one flat mutable struct of hot fields | no |
| Spawner | `Emitter` | spawn schedule, anchor, drive, lifetime policy | no |
| Design | `EffectDef` | named, immutable bundle of emitter specs + curves | no |

### `ParticleEngine` (the manager)
The dumb universal core. Owns the `Pool<Particle>` and live list, runs per-tick
simulation (integrate position, apply forces, advance age, evaluate over-life curves,
recycle dead), enforces budget/caps, and submits render batches. **The only place that
touches pooling, GL/blend state, and cleanup.** Knows nothing about dust or skills.

### `Particle` (simulated unit)
A flat, mutable struct of hot fields — `pos(x,z,height)`, `velocity`, `age`,
`lifetime`, `size`, `rgba`, `blendMode`, `textureRef`, optional `spin`. **No
subclasses.** Flatness is what lets one engine handle everything; `DustParticle extends
Particle` would rebuild the per-type-manager problem inside the pool.

### `Emitter` (spawning mechanism)
Runtime object that produces particles per its schedule, with initial values randomized
inside ranges. Carries:
- an **Anchor** (the *where* — see §5),
- a **Drive** (the *how-much over time* — see §5),
- a **lifetime policy**: continuous (emit forever until stopped) or burst (emit N over a
  window, then die).
`play()` returns an `EmitterHandle` so the caller can `stop()`/`kill()` it.

### `EffectDef` (the design layer)
What you author and name: `EMBER_BURST`, `AMBIENT_DUST`, `CAST_CHARGE`. An effect is
usually **several emitters layered** (e.g. core flash + flying embers + lingering smoke).
Immutable template (see §6). Built via fluent builder (see §4).

## 3. Render integration (the critical bit)

Particles ride the **existing** render pipeline — do not invent a parallel one.
Relevant existing types: `RenderLayer` (`GROUND, BILLBOARD, OVERLAY`; only `BILLBOARD`
is depth-sorted back-to-front by world Z), `SceneRenderable` (`depth()`, `layer()`,
`render(rc)`), `WorldRenderer` (per-layer buckets, cleared and re-collected each frame),
and `SceneCamera` (`project`, `depthScale`).

**Invariant — the emitter is the unit of submission; the particle is the unit of
simulation.** Do NOT submit one `SceneRenderable` per particle (thousands of objects in
the sorted `BILLBOARD` bucket = death). Each emitter submits as **one**
`SceneRenderable` whose `render()` draws all its live particles in a tight internal loop,
and whose `depth()` is the emitter's representative world Z. This gives correct ordering
around entities at emitter granularity without per-particle sorting.

Particles use the same `(groundX, groundZ, height)` convention as entities and project
through `SceneCamera.project` + `depthScale`, inheriting 2.5D fake-height and parallax
for free (deeper particles shrink).

Blend mode lives on the particle/emitter. Skill energy wants additive (`GL_ONE, GL_ONE`);
dust wants normal alpha. Group draw by blend mode within a batch to avoid per-particle
GL state changes.

## 4. Construction — fluent builder is the single path

The builder is the **only** way an `EffectDef` is constructed. Enum / `static final`
constants are just **named handles** onto builder output — never a second construction
route.

```java
EffectDef EMBER_BURST = effect()
    .emitter(e -> e                       // core flash
        .burst(1).lifetime(0.12f).additive()
        .colorOverLife(WHITE, Element.FIRE.tint()))
    .emitter(e -> e                       // flying embers
        .burst(24, 0.1f)
        .speed(range(2f, 5f)).spread(360f).gravity(-3f)
        .lifetime(range(0.4f, 0.8f)).additive()
        .sizeOverLife(Interpolation.pow2In, 0.3f, 0f))
    .build();
```

Effect-level builder adds emitters; each emitter is configured by its own lambda.

**Recommended defaults (confirm on implementation):**
- **`Val` type** — a single constant-or-range value (`Val.of(2f)` / `range(2f,5f)`) with
  `sample(rng)`. Every emitter field takes a `Val`, so you get scalar and ranged forms
  without doubling the method surface. Decide this before the builder grows; it pervades
  every field.
- **Curve helpers** — a thin `Interpolation`-backed set: `constant`, `lerp(a,b)`,
  `lerp(easing,a,b)`, multi-stop `gradient`. Covers nearly every real curve; drop to a
  raw lambda only for the rare bespoke one.
- **Element variants via parametric factory** — `static EffectDef spark(Element e)` runs
  the recipe with `e.tint()`; constants are `EMBER_FIRE = spark(FIRE)`,
  `EMBER_ICE = spark(ICE)`. Bake variants now; only add play-time tint slots if the
  combinatorics explode.

Keep all effects in **one catalog** (e.g. `Vfx` / `ParticleEffects`) of factory methods,
invoked by reference. **Do not** inline spawn loops inside a `SkillInstance` — that
collapses the design layer back into call sites. Tip: build effects in method *bodies*
(not static-field initializers) so JVM hot-swap lets you retune by editing the method
without a restart.

## 5. Play-time inputs — Anchor (where) and Drive (how-much)

Every emitter has two injected inputs, both sampled live each tick, both keeping the
engine ignorant of what a beam or charge meter is.

**Anchor** = the *where*, re-asked every spawn (so a beam can scatter along its length).
Zero-garbage form (see §9):

```java
@FunctionalInterface
public interface Anchor { void point(Vector3 out); }   // writes into caller's buffer

static Anchor at(Vector3 p)         { return out -> out.set(p); }
static Anchor follow(Combatant c)   { return out -> c.worldPos(out); }   // see §9
static Anchor alongBeam(BeamInstance b) {
    return out -> out.set(b.casterPos()).lerp(b.targetPos(), MathUtils.random());
}
```

**Drive** = the *how-much over time*, a `0..1` float supplier. The **emitter envelope**
maps drive → spawn params (rate, speed, spawn radius, tint).

```java
@FunctionalInterface
public interface Drive { float value(); }
```

`play()` signature ties it together:

```java
EmitterHandle play(EffectDef def, Anchor anchor, Drive drive, Channel ch);
```

This three-input model covers the worked examples:
- **Energy flare** — constant drive; the "energy" read is over-life alpha (rise→hot→gone)
  + additive blend + **velocity-aligned stretched quads** (a particle render flag).
- **Powering up** — live drive ramping *up* (`chargeMeter::fraction`); convergence via a
  **point-attractor force** at the anchor. Climactic pop is a *separate* `play()` on
  completion, not part of this emitter.
- **Beam losing power** — live drive ramping *down* off the instance's phase progress;
  one emitter, envelope drops rate/speed/brightness to 0. Already-spawned embers age out
  on their own over-life = the sputter, for free.

New engine features these justify: **emitter envelope** (drive → param curves), **drive
provider** at play-time, **point-attractor force**, **velocity-aligned particles**.

## 6. Immutability & lifecycle

`EffectDef` and its emitter specs are **immutable templates**, shared across every spawn.
The only mutable things are the runtime `Emitter`/`Particle` the engine instantiates
*from* a template when you `play()`. Build-time defines shape and feel; play-time supplies
anchor, drive, and channel.

## 7. Drive ↔ skill-phase sync (engine stays dumb)

**Decision: gameplay owns the clock; the engine reads it.** The `SkillInstance` is the
single source of truth for its phase timeline and exposes a normalized `intensity()`
(or phase + phase-elapsed). The emitter's drive is just `instance::intensity`. The engine
never knows what a beam is.

Two rules make "in sync" automatic and drift-free:
1. **The instance is the only clock**, and it **updates before the engine** each frame, so
   the engine reads a fresh value the same tick:
   ```java
   // CombatSystem.update(dt)
   for (SkillInstance s : active) s.update(dt, ctx);   // clocks advance first
   particles.update(dt);                                // emitters then read drives
   active.removeIf(SkillInstance::isFinished);
   ```
2. **The instance owns the emitter handle** and stops it on phase exit. Already-spawned
   particles finish on their own over-life (`stop()`, not `kill()`).

Sketch (`BeamInstance`, CHARGE→FIRE→FADE):
```java
public float intensity() {
    return switch (phase) {
        case FIRE -> 1f;
        case FADE -> 1f - (t / FADE_DUR);   // ramps 1 → 0
        default   -> 0f;
    };
}
private void enterFire(BattleContext ctx) {
    phase = Phase.FIRE; t = 0;
    embers = ctx.particles.play(Vfx.BEAM_EMBERS, Anchor.alongBeam(this),
                                this::intensity, Channel.COMBAT);
}
private void finish() { phase = Phase.DONE; if (embers != null) embers.stop(); }
```

> Possible future revisit: when skills cast faster under rapid-fire, self-timed envelopes
> may feel better than continuous gameplay-coupling for some effects. Not now.

## 8. Channels — lifecycle/budget control without separate managers

The legitimate part of the "different managers" instinct is that ambient and combat
particles want **different lifecycles and budgets** (clear all combat particles on
battle-end but keep ambient dust; don't let one huge skill starve the atmosphere). The
tool for that is a lightweight **channel** tag on emitters inside the **one** engine —
shared pool, loop, and draw, but per-channel cap and "clear this channel" control.

```java
enum Channel { AMBIENT, COMBAT, UI }   // confirm the set — see open questions
```

Ownership: **ambient emitters live on the environment** (shared across overworld/battle;
persist across battle resets per the project's "keep systems persistent across resets"
principle). **Combat spawns burst emitters.** One engine instance, fed by both.

## 9. Zero-garbage from day one (decided)

Skills will get flashy fast, so bake the no-allocation pattern in now while the API is
tiny. Rule everywhere a hot path produces a vector: **pass a reusable buffer in, write
into it, return nothing.**

- `Anchor.point(Vector3 out)` writes into the caller's vector (§5).
- The emitter owns one `scratch` `Vector3`, reused every spawn:
  ```java
  anchor.point(scratch);            // fills scratch, no new object
  engine.spawn(spec, scratch, k);   // spawn COPIES into the particle
  ```
- **Copy-on-receive contract:** `spawn` must copy out of the passed-in vector into the
  particle's own fields immediately (`particle.pos.set(in)`) and never retain a reference
  — `scratch` mutates on the next spawn.
- **Position getters must not secretly allocate.** Any "give me your position" method on a
  combatant/instance needs a write-into form (`worldPos(Vector3 out)`); a getter that
  returns `new Vector3()` just moves the allocation. Make write-into the convention.

Why: per-spawn allocation at hundreds of particles/sec produces GC churn → frame hitches,
worst exactly during big skills. Buffers in + copy-on-receive = nothing to collect.

## 10. Decisions log & rejected alternatives

**Locked:** one engine + emitters (not manager-per-type); four-level model; channels for
control; **code-defined** effects (not data-driven) for now; fluent builder as the single
construction path with constants as named handles; immutable templates; Anchor + Drive
play-time inputs; phase-coupled drive with a dumb engine; instance owns the clock and
updates before the engine; zero-garbage buffer pattern from the start.

**Rejected / deferred:**
- *libGDX built-in particle system / `.p` files* — adopting it means adopting libGDX's
  engine, emitter model, and editor as a bundle; it draws in its own 2D space and won't
  flow through `SceneCamera` projection, the Z-sort/layer seam, channels, or live drive-
  binding. Sits parallel to gameplay-coupled VFX, not inside it. Could be added later for
  purely decorative fire-and-forget bursts; not the core path.
- *Data-driven (JSON) `EffectDef`s* — deferred, not rejected. **Preserve the seam:** the
  engine consumes spec *objects* and never reads files; skills reference *named effects*
  and never spawn raw particles. A future `ParticleLoader` is then a pure drop-in.
- *Manager-per-type, `Particle` subclasses* — rejected; variety belongs in config.
- *Pixel-quantized particle look* — explored, not adopted.

## 11. Open questions for implementation

1. **`Val` constant-or-range type** vs scalar methods + explicit `.jitter(...)` modifiers.
   (Recommended: `Val`. Settle first — it shapes every emitter method.)
2. **Channel set** — is `AMBIENT` / `COMBAT` enough day one, or is there a real third
   (`UI` sparkle, persistent overworld torches/embers) worth making first-class now?
3. **Ambient parallax** — foreground *and* background dust (two planes around entities),
   or just a single background back-haze? Drives whether a render layer between `GROUND`
   and `BILLBOARD` is needed, or `OVERLAY` reuse for foreground.
4. **Curve representation** — confirm the `Interpolation`-backed helper set vs raw lambdas.

## 12. Suggested build order

Each milestone is independently verifiable.

1. **Core sim** — `Particle` (flat struct), `Pool`, `ParticleEngine` update loop
   (spawn/age/recycle), `Channel` enum. Verify via debug counts; no rendering yet.
2. **Render integration** — per-emitter batch `SceneRenderable`, submit through
   `WorldRenderer` at emitter depth, project via `SceneCamera`. Get one hardcoded burst
   drawing on screen with correct depth ordering.
3. **Emitter + Anchor + `Val`** — static-point burst with ranged initial values.
4. **Fluent builder + curves + catalog** — `effect()...build()`, the curve helpers, a
   `Vfx` catalog with 1–2 effects, the `spark(Element)` variant factory.
5. **Drive + envelope** — wire a beam/charge `SkillInstance.intensity()` to an emitter;
   prove sync via the FADE ramp-down sputter and the update-order rule (§7).
6. **Ambient dust** — continuous region emitter owned by the environment; parallax layer(s);
   persists across battle transitions.
7. **Channels** — per-channel caps + clear-on-battle-end.
8. **Forces & flair** — point-attractor force (power-up convergence), velocity-aligned
   stretched quads (flares).

## 13. Implementation log (live)

Tracks what's actually built vs. the plan above, so a fresh session can continue cold.
Update this section as milestones land.

### Done

**Prerequisites (before any particle code).** Two write-into accessors the design needs,
verified against the codebase:
- **Zero-alloc projection.** `SceneCamera.project(x, z, Vector2 out)` does the math; the old
  allocating `project(x, z)` now delegates to it. Overload threaded through
  `GameEnvironment.project(x, z, out)` and `RenderContext.project(x, z, out)`. Particles call
  the `out` form once per particle per frame → no `Vector2` garbage. (Findings that justified
  this: `BattleContext` already caches battle-tile projections once at resize, so the battle
  hot path allocated nothing; the only true per-frame allocators were two overworld lines.)
- **Overworld callers migrated.** `OverworldScreen.DoorRenderable`/`AvatarRenderable` now share
  one outer-class `projTmp` `Vector2` instead of allocating per draw. (`buildCache` in
  `BattleContext` is deliberately left allocating — it *stores* each `Vector2` in an array, so a
  shared buffer would alias; and it runs once per resize, not per frame.)
- **Ground anchor.** `GridPosition.getWorldX()` + `GridPosition.worldPos(Vector3 out)` (mirror of
  the existing `getWorldZ()`); `Combatant.worldPos(Vector3 out)` is a `default` method delegating
  there (covers `Player`+`Enemy` with no edits to either). **Ceiling:** returns the *tile-snapped*
  ground point, not the smoothed sprite position (`getVisualX/Y` is screen-space). Fine for
  feet-level emission; invert the projection if you ever need sprite-glued anchors mid-dash.

**Milestone 1 — core sim.** `particles/Particle.java` (flat `Pool.Poolable`, reused `Vector3`s +
`Color`, fields: `pos`, `vel`, `age`, `life`, `size`, `color`) and `particles/ParticleEngine.java`
(libGDX `Pool<Particle>` + `Array<Particle> live`; `spawn(...)` returns the `Particle`;
`update(dt)` integrates `pos += vel*dt` backwards-iterating and `pool.free`s the dead).

**Milestone 2 — render integration.** `ParticleEngine implements SceneRenderable`: draws **all**
live particles in one `BILLBOARD` renderable (additive blend, `depthScale`d size, fake height via
`pos.y * depthScale`, linear age fade), submitted in `PlayState.renderWorld()` before `flush`.
Ticked in `PlayState.update()` **after** `combatSystem.update()` (the §7 clock-then-read order).
Verified visually by a temporary debug fountain (`PlayState.spawnDebugFountain()`).

**Milestone 3 — emitter + Anchor + ranged values.** `particles/Val.java` (constant-or-range,
`sample()` via `MathUtils`), `particles/Anchor.java` (`@FunctionalInterface` + `at(x,y,z)` and
`follow(Combatant)` = `c::worldPos`), and `particles/Emitter.java` (CONTINUOUS/BURST modes via
`continuous(...)`/`burst(...)` factories, ranged `speed`/`life`/`size`, cone velocity around +Y by
`spreadDeg`, fractional-accumulator scheduling). `ParticleEngine` now owns an `Array<Emitter>`
(`add(Emitter)`), ticks them at the top of `update(dt)` (spawn-before-integrate), and drops finished
burst emitters. `PlayState`'s debug fountain replaced by a continuous `Emitter`. **`Anchor.alongBeam`
not built yet** (needs a beam instance handle — arrives with M5).

Fixed post-hoc: `Anchor.at` wrote `out.set(x, y, x)` (z arg dropped) — corrected to `(x, y, z)`. (The
`Emitter.main` self-check claimed here was never committed; emitter scheduling is now covered by the
M4 `EffectDef.main` check below instead.)

**Milestone 4 — fluent builder + curves + catalog.** Split the M3 `Emitter` into an immutable
build-time template `particles/EmitterSpec.java` (mode/rate/count/window, ranged `speed`/`life`/`size`,
`spreadDeg`, size + color over-life curves — no anchor, no runtime state) and a runtime
`particles/Emitter.java` = `(spec, anchor)` holding the accumulator/emitted scheduling. `EffectDef.java`
is the immutable, layered bundle: `EffectDef.effect().emitter(e -> …).build()` (the single construction
path — the old public `Emitter.continuous`/`burst` factories are **deleted**), and `play(engine, anchor)`
stamps out one fresh runtime `Emitter` per spec so an effect replays anywhere. `Vfx.java` is the catalog:
`ambientEmbers()` (continuous, now what `PlayState` plays) + `spark(Element)` (burst, white-hot → element
tint, shrinking to nothing) + `tint(Element)`. Over-life curves ride libGDX `Interpolation` — evaluated
in `ParticleEngine.render` (`t = age/life`; size via `sizeInterp.apply(from, from*endScale, t)`; RGB via
one reused `tmp` `Color` lerped `colorFrom`→`colorTo`; alpha still linear `1 − t`). Covered by a `java -ea`
self-check in `EffectDef.main` (burst emits exactly 24 then self-drops; continuous 60/s over 0.5s = 30 live;
size/color curve endpoints).

### Deviations from the plan above (deliberate)

- **`Channel` enum deferred to M7.** It has no behavior until per-channel caps / clear-on-battle-end
  exist; adding it now is tagging spawns for nothing. Arrives with the cap logic.
- **Whole-engine single renderable, one `depth`** (`ParticleEngine.depth`) instead of per-emitter
  submission. The §3 invariant ("never one renderable per particle") is honored; per-*emitter*
  `depth()` granularity is M3's job once emitters exist.
- **Height is a plain `pos.y * depthScale` knob**, not a real projected elevation (`SceneCamera.project`
  hardcodes `y=0`). Tune the constant; upgrade to a projected elevated point only if it reads wrong.
- **M1 assert self-check (`main`) was dropped** when M2 landed. The sim loop (integrate + recycle) has
  no runnable check now — re-add a `java -ea` self-check or a small test if regressions appear.
- **Size over-life is `endScale`-relative, not absolute** (`sizeOverLife(interp, endScale)` ramps the
  *sampled* initial size → `initial × endScale`), unlike the §4 sketch's absolute `from,to`. Chosen so a
  ranged initial size still shrinks proportionally; `endScale = 0` = shrink to nothing.
- **Color over-life is RGB-only; alpha stays the engine's linear `1 − age/life` fade.** Avoids a
  double-fade (curve alpha × engine fade) — author catalog colors opaque. Multi-stop `gradient` from §4 is
  **not built** (YAGNI for two effects); 2-stop `colorOverLife` covers the catalog. No bespoke `Curve`
  class — libGDX `Interpolation` is the curve helper.
- **The per-frame lerped color lives on the engine, not the particle.** M1's scratch `Particle.color` was
  removed; `ParticleEngine` reuses one `tmp` `Color` across the render loop (compute → draw → reuse).

### Next: Milestone 5 — Drive + envelope

Wire a beam/charge `SkillInstance.intensity()` to an emitter and prove sync via the FADE ramp-down
sputter and the §7 update-order rule (instance clocks advance before the engine reads drives). Needs:
`play(...)` returning an `EmitterHandle` (so the instance can `stop()` on phase exit — deferred from M4,
which returns void), a `Drive` (`0..1` supplier) input threaded into the emitter envelope (drive → spawn
params), and `Anchor.alongBeam(BeamInstance)` (the one anchor M3 left unbuilt).

**Still deferred — per-emitter rendering.** Each emitter owning its particle list and submitting its
own `SceneRenderable` at its own `depth()` (replacing the single whole-engine renderable) is not done.
Do it when multiple emitters at different depths coexist (M5 skills / M6 ambient), since that's when
one shared `depth` visibly mis-sorts.

### Owner / lifecycle still open

For M2 the engine is instantiated in `PlayState` (per-battle, GC'd with it). Per §8 the real home is
**one engine on the environment** (shared overworld+battle, persists across resets) for ambient, with
combat spawning into it. Resolve the single `engine.update()` owner per screen before building channels
(M7) — in battle it must tick after `combatSystem.update()`; the overworld has no `CombatSystem`.

### Data-driven skill VFX — the `vfx` list

Skills list their particle effects by name in `skills.json`; every shape layers them. The old hardcoded
beam pick (`element == ICE ? beamMist : beamEmbers` in `BeamInstance`) is gone — it's now data.

- **Catalog names.** `Vfx.byName(String)` maps a skill-facing name → `VfxFactory`
  `(Element, Color tint, int dir) -> EffectDef`. Registered: `beamEmbers`, `beamIceMist`, `spark`, `crackle`. Unknown
  names throw, and `SkillLoader.parseVfx` wraps that in the usual skill-id error → **bad data fails at
  load, not mid-battle**. Ambient effects (`ambientDust`/`ambientEmbers`) are deliberately *not*
  registered — they're environment, not skill-triggered.
- **On the skill.** `Skill.vfx` is a resolved `List<VfxFactory>` (empty = no particle VFX), parsed from
  `"vfx": ["beamIceMist", "spark"]`. Tint comes from the skill's `vfxTint`, element from its `element`,
  direction from the firing instance.
- **The shared hook.** `SkillInstance.playVfx(Anchor, Drive)` (+ `playVfx(Anchor)` = `Drive.FULL`) plays
  every listed effect at `Channel.COMBAT`, stashing handles; base `onFinish()` stops them all (spawning
  halts, live particles age out — so one path handles both continuous *and* burst effects). Subclasses
  overriding `onFinish()` **must call `super`**. `tileAnchor(col,row)` is the shared ground-point anchor.
- **Per-shape trigger + anchor** (each shape calls `playVfx` once, at its acting moment):
  Beam → `beamPoint` scatter + `intensity` drive, on FIRE. Strike → struck tile, on HIT. Projectile →
  **travel trail** on `trailPoint`, played at launch and handle-stopped at impact/edge/landing so the
  tail ages out along the path. Aura → `Anchor.follow(caster)`, on ACTIVE. Zone → target tile, on
  ACTIVE. All anchors are grid-world space (`GridPosition.worldPos`'s `(floorX, height, floorZ)`).

### Screen → world: the unproject helpers

Projectiles fly in *screen space* (`posX/posY` viewport units); particles anchor in *world* space.
The bridge is `SceneCamera.unprojectX(viewportX, worldZ)` / `unprojectHeight(viewportY, worldX, worldZ)`
(delegated via `GameEnvironment`) — the inverse of the **billboard convention** (`drawn = project(x,z)
+ height·depthScale(z)`), not of the raw camera. It lives on `SceneCamera` because forward and inverse
of the same convention must change together; it's grid-agnostic (probe points, no tile cache — the
`BattleContext` tile cache serves *many repeated identical* tile queries, the inverse serves *few
continuous* ones; memoize probe pairs inside SceneCamera if it ever profiles hot). Consumers:
`CombatSystem.spawnClash` (clash midpoint) and `ProjectileInstance.trailPoint` (the moving trail
anchor — sprite center → world each spawn; lob arc height falls out of `unprojectHeight` naturally).

### Texture sets — non-uniform particles

An emitter draws from a **set** of textures, not one: `EmitterSpec.textures` is a `Texture[]`. How the
set is used is a fluent builder choice, matching the `sizeOverLife`/`colorOverLife` idiom:

- `texture(t)` — one texture, uniform effect (sugar for a list of one; the common case).
- `textures(t1, t2, …)` — **random per particle**: each particle picks an index at spawn
  (`Particle.texIndex`) and keeps it for life. Spatial variety — `crackle(Element)` scatters all seven
  `spark_01..07` shards (`assets.spark(1..7)`) so the lightning off a thunder beam isn't one repeated
  sprite.
- `texturesOverLife(t1, t2, …)` — **ordered cycle**: each particle plays through the set *in the given
  order* over its life (frame = `age/life`; `texIndex` is ignored). Temporal evolution —
  `beamIceMist` morphs each puff through `smoke_01..10` as it blooms and fades.

Conventions:

- **The default texture is invisible.** `EmitterSpec.build()` substitutes a static default
  (`EmitterSpec.init(generated.pixel())`, wired once in `Main.create()`) when no texture was set, so
  `spec.textures` is *guaranteed* non-null/non-empty — no downstream null checks in `Emitter`,
  `ParticleEngine`, or `Vfx`, ever. The engine no longer owns a fallback texture (constructor is
  arg-less now).
- **Order is authored at the effect build site.** `GameAssets` exposes the effect sets individually
  (`spark(int)` / `star(int)` / `smoke(int)` / `circle(int)` / `flare(int)`, 1-based over their
  `*_SET` arrays) rather than as pre-bundled arrays, so a catalog method picks *which* frames and
  *in what order* right where the effect is defined (see `Vfx.beamIceMist`, `Vfx.crackle`). The old
  semantic aliases (`dust`/`ember`/`star04`) are gone — the catalog names the role in a comment at
  the call site.

### The impact recipe — layered burst on one anchor

`Vfx.impact(Element)` replaced the old `ClashEffect` sprite at the projectile-vs-projectile clash
(`CombatSystem.spawnClash`). It's the house pattern for "big hit" effects: **six emitters in one
`EffectDef`, all stamped onto the same anchor** (the meeting point), with staggered lifetimes doing
the storytelling — flash (frames) → ring/sparks (beats) → sparkles/smoke (linger):

1. Core flash — `burst(1)`, one huge `circle(1)` pop, ~0.12s, white→tint.
2. Flare streak — `burst(1)`, `flare(1)` lens-glint, ~0.18s.
3. Shockwave ring — `burst(1)`, `circle(5)` blowing out to 3.5× (`pow2Out`), ~0.3s.
4. Debris sparks — `burst(18)`, random `spark(1..7)`, fast radial, shrink to 0.
5. Sparkles — `burst(10)`, random `star(1/4/9)`, slow + up-drift, 0.5–0.9s.
6. Smoke — `burst(5)`, `texturesOverLife(smoke …)`, alpha-blended, billows, 0.8–1.4s tail.

Notes: the clash midpoint is screen-space (projectiles track screen x), but anchors are grid-world —
at fixed z the projection is linear in x, so `spawnClash` inverts it from two known tiles. The burst
centers at `IMPACT_HEIGHT` (~0.35) above the floor. `ClashEffect` the class survives only for
`StrikeInstance`'s slash sprites; `clash.png`/`clashTexture` are gone.

### Aura recipes — heal / powerUp / magicUp (sprite-less, on the caster)

The three buff/heal auras are pure particles: their skills list `"vfx"` and **omit `vfxTexture`** —
allowed only for AURA shape with a non-empty vfx list (`Skill.build()` enforces; every other shape
still fails at load without a sprite, since Projectile/Beam/Zone build `Sprite`s from it).
`AuraInstance` skips its sprite pass when the texture is null; timing/effects logic is untouched.

- `heal` — glow bloom (`light(1)`) + ground ring (`circle(5)`) + hearts (`symbol(1)`, alpha-blended,
  rising) + star motes. All bursts: a gentle ~1s cast moment.
- `powerUp` — flash (`circle(1)`) + shockring intro, then **continuous** rising flames
  (`textures(flame(1..6))`) and `star(8)` embers that burn for the whole 10s buff.
- `magicUp` — ONE big `magic(1)` pentagram stamp + translucent `twirl` veil, then **continuous**
  arcane glints (`magic(3)/magic(4)/star(9)`), plus a purple glow bloom.

Two supporting pieces introduced here:
- **`.jitter(hx, hy, hz)`** (`EmitterSpec.Builder`) — per-spawn random offset off the anchor point,
  so body effects spread across the caster instead of fountaining from the feet-center. Default 0.
- **`.offset(x, y, z)`** — constant shift off the anchor, per emitter — with **`.atBody()`** as the
  named sugar (`offset(0, BODY_Y, 0)`, `BODY_Y = 0.45`): heal's glow/hearts/motes ride at mid-torso
  while its ground-ring layer stays at the feet, all on one anchor.
- **Continuous emitters ride the aura's lifetime for free**: `playVfx` stores the handles and base
  `onFinish()` stops them, so a 10s buff emits for 10s and sputters out on fade — no timers needed.

### voidPull & regen

- `voidPull` (zone tile, rides the zone's 6s) — contracting `twirl` swirl + **four inward streams**
  + a `light(1)` singularity heart. The streams are the house **suction idiom**: spawn `offset` one
  tile out (real dims via the now-static `Battlefield.panelFloorWidth()/panelFloorDepth()`), `drift`
  aimed at the center sized to cross one tile per average lifetime, and `sizeOverLife → 0` so motes
  are swallowed exactly on arrival. Deliberately restrained (~27 live at steady state).
- `regen` (sprite-less aura like powerUp/magicUp, rides the 4s REGEN status) — a dim breathing
  `light(1)` halo `.atBody()` (overlapping soft copies read as a steady sprite glow) + a few rising
  motes. Quieter than heal's cast bloom by design.
- `fireTrail` (fire_blast — the sprite stays as the core; this is everything coming off it):
  `light(1)` glow hugging the ball (short-life so it doesn't smear) + `flare(1)` glint pulses +
  `flame(1..6)` tongues licking backward (`drift(-dir…)`) + `star(8)` embers + alpha-blended smoke
  wisps at the tail end. Rides `trailPoint`, so the ball's motion draws the trail; ~30 live.
