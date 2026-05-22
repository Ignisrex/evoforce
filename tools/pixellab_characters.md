# PixelLab character manifest

Enemy/creature sprites are generated via the **PixelLab MCP** (`create_character` /
`create_character_state`), not the REST `generate_sprites_pixellab.py` script (that
older script produced single painterly billboards and is kept only for reference).

All characters: **Pro mode**, **128px**, **`low top-down`** view (matches the HD-2D
"three-quarter from slightly above" billboard look), 8 directions.

## Orientation convention

The battle grid puts the **player on the west side** (cols 0–3) and **enemies on the
east side** (cols 4–7), camera looking from the front. So sprites are angled toward
the opposing side:

- `<name>_se.png` — **south-east** = player-side facing (angled toward enemy territory)
- `<name>_sw.png` — **south-west** = enemy-side facing (angled toward the player)
- `<name>.png` — the bare file the game currently loads, copied from the orientation
  matching its current use: `beastkin` = SE (player avatar), everything else = SW
  (enemies). A future side-aware renderer can pick `_se`/`_sw` per grid side (or just
  horizontally flip one frame).

## Lineage groups

Evolutions are `create_character_state` derivations, so each line shares a PixelLab
`group_id` and the evolved form inherits the base's identity. Lore parents that span
two lines (the convergent/bridge nodes) can't be encoded in a single-parent state, so
they're noted here only.

```
beastkin (root)          skeleton (root)
lich ─► elder_lich ─► lich_king        group c1f15afe
lycan ─► fenrir                        group 945aa5b4
lionen ─┬─► nemean                     group 22a7efad
        └─► undead_beastkin            (bridge: lore-fed by beastkin + lionen → lich)
eclipse_beast (standalone quadruped, lion template; lore convergence of fenrir + nemean)
```

## Character IDs

| name            | character_id                          | body / template   | parent (state of) | group_id  |
|-----------------|---------------------------------------|-------------------|-------------------|-----------|
| beastkin        | 0e116fa7-867c-45c2-937f-bbc10f187e1e  | humanoid          | — (root)          | —         |
| skeleton        | 5d7404ef-1a95-4453-91a1-1f061c5006b1  | humanoid          | — (root)          | —         |
| lich            | 05992226-78f3-42fd-8aec-c9c6c6b0ea64  | humanoid          | — (base)          | c1f15afe  |
| elder_lich      | 35f92d60-374d-4fad-a750-2d0ecff03a16  | humanoid          | lich              | c1f15afe  |
| lich_king       | 1bb00df2-81be-4a91-8701-1e3e97035479  | humanoid          | elder_lich        | c1f15afe  |
| lycan           | a56a7a88-ed97-4ff2-b177-120b5c1196d0  | humanoid          | — (base)          | 945aa5b4  |
| fenrir          | 8c0d4d50-232a-472a-bc46-27cfa9cb958e  | humanoid          | lycan             | 945aa5b4  |
| lionen          | b568fa66-136f-4192-b5cc-7483a4141fb8  | humanoid          | — (base)          | 22a7efad  |
| nemean          | 20388c0f-1da8-478b-96a3-94b0f815f5c6  | humanoid          | lionen            | 22a7efad  |
| undead_beastkin | 6dafa2e7-5b18-43c4-a517-1a616b1d7e1b  | humanoid          | lionen            | 22a7efad  |
| eclipse_beast   | e59249e7-a388-4a8d-a001-43963f8ef99f  | quadruped (lion)  | — (standalone)    | —         |

Re-download any rotation with:
`https://backblaze.pixellab.ai/file/pixellab-characters/c688be2f-c859-4ab6-8aee-da6f5fa4cabd/<character_id>/rotations/<direction>.png`
(directions: south, south-east, south-west, east, west, north, north-east, north-west)

**After downloading, run `python tools/normalize_sprites.py`.** PixelLab frames
the character on a canvas ~40% larger than the sprite (content fills only
~37-48%), which makes it render small in the fixed tile square. The script
autocrops the transparent margin and re-centers each frame onto a tight square,
feet flush to the bottom (the billboard is bottom-anchored), preserving aspect.
