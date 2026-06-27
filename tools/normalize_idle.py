"""
Normalize idle spritesheet strips for the runtime billboard renderer.

PixelLab exports each frame on an oversized canvas, so the character only fills
~35-48% of the cell (centered, with large transparent margins below the feet).
The game draws every billboard into a fixed `pw x pw` world quad, so that padding
makes monsters render small and floating.

This script tight-crops each monster's idle strip to a single, consistent SQUARE
box (the union of every frame's alpha bounding box across BOTH facings, expanded
to a square and bottom-anchored on the feet), then writes square frames where the
character fills the cell. Square frames keep GameAssets.sliceAnimSet's
`cell = sheet.getHeight()` assumption valid, so no code change is needed.

Source (pristine):  tools/pixellab_raw/<monster>/idle/<se|sw>/spritesheet.png
Dest (normalized):  assets/sprites/<monster>/idle/<se|sw>.png

Re-run this whenever the raw idle art is regenerated.
"""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "tools", "pixellab_raw")
DEST = os.path.join(ROOT, "assets", "sprites")

MONSTERS = [
    "beastkin", "eclipse_beast", "elder_lich", "fenrir", "lich", "lich_king",
    "lionen", "lycan", "nemean", "skeleton", "undead_beastkin",
]
FACINGS = ["se", "sw"]
MARGIN = 2  # px of breathing room around the union box before squaring


def frames(im, cell):
    n = im.width // cell
    return [im.crop((i * cell, 0, (i + 1) * cell, cell)) for i in range(n)]


def strip_bbox(im, cell):
    """Union of per-frame alpha bboxes within ONE facing's strip (preserves its
    own sway; computed per facing because se/sw sit at different canvas x)."""
    minx = miny = cell
    maxx = maxy = 0
    found = False
    for f in frames(im, cell):
        bb = f.getchannel("A").getbbox()
        if bb is None:
            continue
        found = True
        minx, miny = min(minx, bb[0]), min(miny, bb[1])
        maxx, maxy = max(maxx, bb[2]), max(maxy, bb[3])
    if not found:
        raise ValueError("all frames fully transparent")
    return minx, miny, maxx, maxy


def place(bbox, side, cell):
    """Bottom-anchored, horizontally-centered square crop of `side` within a cell."""
    minx, miny, maxx, maxy = bbox
    bottom = min(cell, maxy + MARGIN)            # keep feet on the floor
    top = max(0, bottom - side)
    cx = (minx + maxx) // 2
    left = max(0, min(cx - side // 2, cell - side))
    return left, top


def process(monster):
    raws, bboxes, cell = {}, {}, None
    for d in FACINGS:
        src = os.path.join(RAW, monster, "idle", d, "spritesheet.png")
        im = Image.open(src).convert("RGBA")
        raws[d] = im
        cell = im.height
        bboxes[d] = strip_bbox(im, cell)
    # Shared square side so both facings render at the same scale; sized to the
    # character itself (max single-facing extent), NOT the se+sw union.
    side = min(cell, max(max(b[2] - b[0], b[3] - b[1]) for b in bboxes.values()) + 2 * MARGIN)
    for d, im in raws.items():
        n = im.width // cell
        left, top = place(bboxes[d], side, cell)
        out = Image.new("RGBA", (n * side, side), (0, 0, 0, 0))
        for i in range(n):
            box = (i * cell + left, top, i * cell + left + side, top + side)
            out.paste(im.crop(box), (i * side, 0))
        dst = os.path.join(DEST, monster, "idle", f"{d}.png")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        out.save(dst)
    print(f"{monster:16s} cell={cell} -> {side}x{side} ({n} frames)")


if __name__ == "__main__":
    for m in MONSTERS:
        process(m)
