# Procedural Cave Polish — Design

**Date:** 2026-08-11
**Status:** Approved direction, pending implementation plan

## Goal

Make the 3D cave environment read as a detailed, atmospheric place instead of a
textured graybox — while staying fully procedural (no model loading, no new
dependencies) and preserving every existing gameplay contract.

Pain points being addressed, all four confirmed: boxy geometry, flat
atmosphere, nothing moves, sparse set dressing.

## Decisions (from brainstorming)

- **Scope:** cave first, structured so future biomes are a data swap, not a rework.
- **Visual bar:** detailed 3D geometry (rock relief, real formations), not
  atmosphere-only.
- **Geometry source:** pure procedural — `MeshBuilder` + seeded noise. No
  gdx-gltf, no model files, no conversion tooling. (The Meshy GLB experiment was
  rejected 2026-08-08; this deliberately avoids that path.)
- **Textures:** store-bought CC0 seamless rock textures (Poly Haven /
  ambientCG), AO baked into diffuse offline. Geometry is generated; surface
  detail is bought.
- **Variation:** seeded per battle. Overworld uses a fixed seed.

## Architecture

### 1. Theme + seed plumbing (foundation — built first)

New `CaveTheme` data class in `environment/`:

- wall texture, floor texture (+ optional floor emissive)
- fog color, ambient color, light palette (list of point-light colors)
- decor density knobs (formation count ranges, scatter density)
- seed (long)

`GameEnvironment.buildGeometry()` becomes `rebuild(CaveTheme)`: dispose all
owned models, rebuild from the theme. Overworld passes a fixed-seed theme;
battle entry passes a random-seed theme. A second biome later is another
`CaveTheme` instance — no framework.

### 2. Geometry (stage 1)

- **Walls + ceiling:** replace flat boxes with subdivided mesh grids
  (`MeshBuilder`), vertices displaced along the surface normal by seeded value
  noise (2–3 octaves), normals recomputed after displacement. Produces rock
  relief and an uneven silhouette.
- **Floor:** the arena plane stays perfectly flat — `SceneCamera.project()`,
  billboard grounding, and `BattlefieldDecor` panels depend on it. A displaced
  "skirt" mesh surrounds it, rising toward the walls, so the flat center reads
  as worn cave floor.
- **Formations:** stalagmite/stalactite clusters (2–4 jittered, tilted cones
  merged per cluster) replace the current lone cones. Crystal shard clusters
  (stretched, tilted prisms) carry emissive materials.
- **Textures:** seamless CC0 rock sets. A small Pillow step in `tools/`
  multiplies the AO map into the diffuse. Diffuse-only materials — no normal
  maps (unreliable on the stock `DefaultShader`).

### 3. Atmosphere (stage 2)

- Fog: `ColorAttribute.Fog` on the existing `Environment` — supported by the
  stock shader, color from theme, tuned against camera far plane.
- Point lights repositioned to sit at crystal clusters (light visibly comes
  from sources); colors from theme palette.
- Crystal emissive materials + the existing bloom pass produce glow. No new
  post-processing.

### 4. Motion (stage 3)

`GameEnvironment` gains `update(float delta)`, called by both `OverworldScreen`
and `PlayState`:

- Point-light intensity flicker (small per-frame jitter).
- Crystal emissive pulse (slow sine on the emissive color attribute).
- Dust motes: a handful of slow-drifting billboards drawn in the existing 2D
  sprite pass via `project()`. No particle system.

### 5. Set dressing (stage 4)

Seeded scatterer placing small procedural meshes (rocks, small crystals,
mushrooms) with three rules:

- Keep-out zone over the battle grid and the overworld walkable area (bounds
  taken from existing movement bounds, not new config).
- Density increases toward walls.
- Seeded size/rotation/tint variation.

## Build order

Theme plumbing first, then geometry → atmosphere → motion → set dressing.
Each stage is independently shippable and visibly improves the scene.

## Constraints

- Flat gameplay plane preserved by construction; `SceneCamera` and
  `BattlefieldDecor` untouched.
- Single `ModelBatch` pass, stock shader only. `BattlefieldDecor`'s material
  mutation keeps working.
- Per-battle rebuild must dispose cleanly — reuse the existing
  `models`/`decorModels` ownership split.
- Camera stays fixed (posed once at construction), per current SPEC.

## Error handling

- Missing/null emissive texture already tolerated; theme fields follow the same
  pattern (null optional texture → skip attribute).
- Rebuild during a battle never happens — theme is set on screen entry only.

## Verification

No test suite exists in this project; verification is visual. Run the game
after each stage (`gradlew.bat lwjgl3:run`), compare against pre-change
screenshots, check both battle and overworld (shared environment), and confirm
no leaks across repeated battle entries (rebuild path).

## Out of scope

- Model/asset loading of any kind (gdx-gltf stays out).
- Custom shaders (triplanar, vertex animation).
- Camera motion / screen shake.
- A biome framework beyond the `CaveTheme` data class.
