"""Generate a Mega Man Battle Network-style 6x3 battlefield texture.

Layout (as viewed on screen):
    +---+---+---+---+---+---+
    | B | B | B | R | R | R |   row 0 (back)
    +---+---+---+---+---+---+
    | B | B | B | R | R | R |   row 1 (middle)
    +---+---+---+---+---+---+
    | B | B | B | R | R | R |   row 2 (front)
    +---+---+---+---+---+---+
          player side    enemy side

The image is rendered top-down (no perspective). Tilt/scale it in-engine
if you want the classic MMBN 3D look.

Usage:
    python tools/generate_battlefield.py
    python tools/generate_battlefield.py --panel 192 128 --out assets/battlefield.png
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw


COLS = 6
ROWS = 3

# MMBN-ish palette.
PLAYER_TOP      = (120, 190, 255)   # light cyan/blue
PLAYER_MID      = (60, 130, 220)
PLAYER_BOTTOM   = (20,  60, 150)
PLAYER_EDGE_HI  = (200, 235, 255)
PLAYER_EDGE_LO  = (10,  30,  80)

ENEMY_TOP       = (255, 150, 130)   # light red/orange
ENEMY_MID       = (220,  70,  60)
ENEMY_BOTTOM    = (140,  20,  30)
ENEMY_EDGE_HI   = (255, 210, 200)
ENEMY_EDGE_LO   = (80,   10,  20)

SEAM_COLOR      = (8, 10, 18)       # near-black gap between panels
BG_COLOR        = (0, 0, 0, 0)      # transparent outside the grid


def v_gradient(width: int, height: int, top, mid, bottom) -> Image.Image:
    """Simple 3-stop vertical gradient panel."""
    img = Image.new("RGB", (width, height), mid)
    px = img.load()
    for y in range(height):
        t = y / max(1, height - 1)
        if t < 0.5:
            k = t / 0.5
            r = int(top[0] + (mid[0] - top[0]) * k)
            g = int(top[1] + (mid[1] - top[1]) * k)
            b = int(top[2] + (mid[2] - top[2]) * k)
        else:
            k = (t - 0.5) / 0.5
            r = int(mid[0] + (bottom[0] - mid[0]) * k)
            g = int(mid[1] + (bottom[1] - mid[1]) * k)
            b = int(mid[2] + (bottom[2] - mid[2]) * k)
        for x in range(width):
            px[x, y] = (r, g, b)
    return img


def draw_panel(panel_w: int, panel_h: int, is_player: bool) -> Image.Image:
    top, mid, bot, hi, lo = (
        (PLAYER_TOP, PLAYER_MID, PLAYER_BOTTOM, PLAYER_EDGE_HI, PLAYER_EDGE_LO)
        if is_player
        else (ENEMY_TOP, ENEMY_MID, ENEMY_BOTTOM, ENEMY_EDGE_HI, ENEMY_EDGE_LO)
    )

    panel = v_gradient(panel_w, panel_h, top, mid, bot).convert("RGBA")
    d = ImageDraw.Draw(panel)

    bevel = max(2, panel_h // 16)

    # Outer beveled border: light on top/left, dark on bottom/right.
    for i in range(bevel):
        # top edge
        d.line([(i, i), (panel_w - 1 - i, i)], fill=hi)
        # left edge
        d.line([(i, i), (i, panel_h - 1 - i)], fill=hi)
        # bottom edge
        d.line([(i, panel_h - 1 - i), (panel_w - 1 - i, panel_h - 1 - i)], fill=lo)
        # right edge
        d.line([(panel_w - 1 - i, i), (panel_w - 1 - i, panel_h - 1 - i)], fill=lo)

    # Inner inset rectangle for that chiseled MMBN look.
    inset = bevel + max(2, panel_h // 20)
    d.rectangle(
        [inset, inset, panel_w - 1 - inset, panel_h - 1 - inset],
        outline=lo,
        width=1,
    )

    # Subtle center highlight (diamond) — very faint.
    cx, cy = panel_w // 2, panel_h // 2
    rx, ry = panel_w // 5, panel_h // 5
    d.polygon(
        [(cx, cy - ry), (cx + rx, cy), (cx, cy + ry), (cx - rx, cy)],
        outline=(hi[0], hi[1], hi[2], 90),
    )

    return panel


def generate(panel_w: int, panel_h: int, seam: int, out_path: Path) -> None:
    img_w = COLS * panel_w + (COLS + 1) * seam
    img_h = ROWS * panel_h + (ROWS + 1) * seam
    img = Image.new("RGBA", (img_w, img_h), SEAM_COLOR + (255,))

    player_panel = draw_panel(panel_w, panel_h, is_player=True)
    enemy_panel = draw_panel(panel_w, panel_h, is_player=False)

    for row in range(ROWS):
        for col in range(COLS):
            panel = player_panel if col < COLS // 2 else enemy_panel
            x = seam + col * (panel_w + seam)
            y = seam + row * (panel_h + seam)
            img.paste(panel, (x, y), panel)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path, "PNG")
    print(f"Wrote {out_path} ({img_w}x{img_h})")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--panel",
        nargs=2,
        type=int,
        metavar=("W", "H"),
        default=(160, 96),
        help="panel size in pixels (default: 160 96)",
    )
    parser.add_argument(
        "--seam",
        type=int,
        default=4,
        help="gap between panels in pixels (default: 4)",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("assets") / "battlefield.png",
        help="output PNG path (default: assets/battlefield.png)",
    )
    args = parser.parse_args()
    generate(args.panel[0], args.panel[1], args.seam, args.out)


if __name__ == "__main__":
    main()
