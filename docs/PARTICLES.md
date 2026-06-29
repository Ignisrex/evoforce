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

### Next: Milestone 3 — emitter + Anchor + ranged values

Replace `spawnDebugFountain()` with a real `Emitter` (spawn schedule + `Anchor` + lifetime policy:
continuous/burst) and ranged initial values. **Delete the debug fountain** (`spawnDebugFountain()`
call + method, and the temporary `particles.depth` line) when the emitter drives spawns. This is
also where per-emitter submission (each emitter its own `SceneRenderable` at its own `depth()`)
should replace the whole-engine renderable.

### Owner / lifecycle still open

For M2 the engine is instantiated in `PlayState` (per-battle, GC'd with it). Per §8 the real home is
**one engine on the environment** (shared overworld+battle, persists across resets) for ambient, with
combat spawning into it. Resolve the single `engine.update()` owner per screen before building channels
(M7) — in battle it must tick after `combatSystem.update()`; the overworld has no `CombatSystem`.
