"""Stitch PixelLab animation frames into row-major per-monster sprite sheets.

Reads:  tools/pixellab_raw/<monster>/<state>/<direction>/<N>.png
Writes: assets/sprites/<monster>/<monster>_<direction>.png

The sheet is laid out as rows of state, columns of frames:
    Row 0: IDLE   frames
    Row 1: MOVE   frames
    Row 2: ATTACK frames
    Row 3: CAST   frames
    Row 4: HURT   frames
    Row 5: DEATH  frames

Cell size = max(frame size) across all states for that monster's direction,
so heterogeneous frame sizes still align. Rows with fewer frames than the
sheet width are padded transparent.

The actual frame count per state is declared in Monster.java's AnimSheet --
this script just stitches whatever frames it finds. If a state has no frames
for a direction, that row is left fully transparent (and the AnimSheet
declaration in Java should fall back to IDLE for that state).

Each frame is autocropped + re-centered + bottom-anchored before being placed
(same recipe as normalize_sprites.py) so the billboard ground-contact stays
correct across the animation.

Usage:
    python tools/build_anim_sheets.py                  # every monster with raw frames
    python tools/build_anim_sheets.py beastkin lich    # just these monsters
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

RAW_DIR     = Path("tools") / "pixellab_raw"
SPRITES_DIR = Path("assets") / "sprites"

STATES = ["idle", "move", "attack", "cast", "hurt", "death"]
DIRECTIONS = ["se", "sw"]


def state_frames(monster: str, state: str, direction: str) -> list[Path]:
    """Sorted list of frame PNGs for one (monster, state, direction)."""
    d = RAW_DIR / monster / state / direction
    if not d.is_dir():
        return []
    return sorted(p for p in d.glob("*.png"))


def normalize_frame(im: Image.Image) -> Image.Image:
    """Crop transparent margin, re-center on a tight square, feet bottom-anchored."""
    bbox = im.getbbox()
    if bbox is None:
        return im
    content = im.crop(bbox)
    cw, ch = content.size
    side = max(cw, ch)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(content, ((side - cw) // 2, side - ch), content)
    return canvas


def stitch(monster: str, direction: str) -> bool:
    """Build assets/sprites/<monster>/<monster>_<direction>.png. Returns True if written."""
    rows: list[list[Image.Image]] = []
    for state in STATES:
        paths = state_frames(monster, state, direction)
        row = [normalize_frame(Image.open(p).convert("RGBA")) for p in paths]
        rows.append(row)

    if not any(rows):
        print(f"  {monster}_{direction}: no frames")
        return False

    cell = max(im.size[0] for row in rows for im in row)
    max_frames = max(len(row) for row in rows)
    sheet_w = cell * max_frames
    sheet_h = cell * len(STATES)
    sheet = Image.new("RGBA", (sheet_w, sheet_h), (0, 0, 0, 0))

    for ri, row in enumerate(rows):
        y = ri * cell
        for ci, frame in enumerate(row):
            if frame.size != (cell, cell):
                frame = frame.resize((cell, cell), Image.NEAREST)
            sheet.paste(frame, (ci * cell, y), frame)

    out_dir = SPRITES_DIR / monster
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{monster}_{direction}.png"
    sheet.save(out_path)
    counts = [len(r) for r in rows]
    print(f"  {monster}_{direction}: {sheet_w}x{sheet_h} (cell {cell}), frames={counts}")
    return True


def main() -> None:
    wanted = sys.argv[1:]
    if not wanted:
        wanted = sorted(d.name for d in RAW_DIR.iterdir() if d.is_dir()) if RAW_DIR.exists() else []
    if not wanted:
        print(f"No monsters found under {RAW_DIR}/")
        return
    for monster in wanted:
        print(monster)
        for direction in DIRECTIONS:
            stitch(monster, direction)


if __name__ == "__main__":
    main()
