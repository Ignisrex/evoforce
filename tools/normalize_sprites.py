"""Normalize PixelLab character frames for in-game billboard rendering.

PixelLab returns each rotation on a canvas ~40% larger than the character,
with lots of transparent margin (the character fills only ~37-48% of the
frame). The battle renderer draws the whole frame into a fixed tile square, so
that padding makes sprites look small and inconsistently sized.

This script autocrops each frame to its alpha bounding box, then re-centers it
onto a tight square canvas (side = the larger content dimension) with the feet
flush to the bottom edge -- the billboard is bottom-anchored at the tile, so
"feet at bottom" keeps the ground contact correct. Aspect ratio is preserved
(the renderer draws into a square), so no distortion.

Idempotent: re-running on already-normalized frames is a no-op.

Usage:
    python tools/normalize_sprites.py            # all generated sprites
    python tools/normalize_sprites.py lich fenrir # just these basenames
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

SPRITES_DIR = Path("assets") / "sprites"

# The creatures generated via the PixelLab MCP (see tools/pixellab_characters.md).
# spider.png / werewolf.png are intentionally excluded.
BASENAMES = [
    "beastkin", "skeleton",
    "lich", "elder_lich", "lich_king",
    "lycan", "fenrir",
    "lionen", "nemean", "undead_beastkin",
    "eclipse_beast",
]
SUFFIXES = ["", "_se", "_sw"]


def normalize(path: Path) -> None:
    im = Image.open(path).convert("RGBA")
    bbox = im.getbbox()
    if bbox is None:
        print(f"  skip (empty) {path.name}")
        return
    content = im.crop(bbox)
    cw, ch = content.size
    side = max(cw, ch)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    # Horizontally centered, bottom-aligned (feet on the bottom edge).
    canvas.paste(content, ((side - cw) // 2, side - ch), content)
    canvas.save(path)
    print(f"  {path.name:24} {im.size[0]}x{im.size[1]} -> {side}x{side} (content {cw}x{ch})")


def main() -> None:
    wanted = sys.argv[1:] or BASENAMES
    count = 0
    for base in wanted:
        for suf in SUFFIXES:
            p = SPRITES_DIR / f"{base}{suf}.png"
            if p.exists():
                normalize(p)
                count += 1
    print(f"Normalized {count} sprite(s).")


if __name__ == "__main__":
    main()
