"""Generate a small starburst sprite used as the projectile-clash VFX.

The sprite is drawn procedurally with Pillow: a bright white core fading
through pale yellow into orange at the edge, plus a soft 8-spoke radial
starburst on top. Background is fully transparent so the sprite can be
scaled / faded by the engine when it's spawned at a clash point.

Usage:
    python tools/generate_clash.py
    python tools/generate_clash.py --size 192 --out assets/effects/clash.png
"""

from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image


DEFAULT_SIZE = 128
SPOKES = 4  # cos(angle * SPOKES) yields 2 * SPOKES alternating peaks → 8 rays.


def generate(out: Path, size: int = DEFAULT_SIZE) -> None:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = cy = size / 2.0
    max_r = size / 2.0

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            if r > max_r:
                continue
            t = r / max_r  # 0 at center, 1 at edge

            # Soft circular glow that falls off quadratically.
            base_alpha = (1.0 - t) ** 2

            # Radial spokes: keeps only angular peaks of cos(angle * SPOKES).
            angle = math.atan2(dy, dx)
            spoke = max(0.0, abs(math.cos(angle * SPOKES)) - 0.7) / 0.3
            spoke_alpha = spoke * (1.0 - t)

            alpha = max(base_alpha, spoke_alpha)
            if alpha <= 0:
                continue

            # Color ramp: white center → pale yellow → orange edge.
            if t < 0.35:
                rr, gg, bb = 255, 255, 255
            elif t < 0.7:
                k = (t - 0.35) / 0.35
                rr = 255
                gg = int(255 - 15 * k)
                bb = int(255 - 95 * k)
            else:
                k = (t - 0.7) / 0.3
                rr = 255
                gg = int(240 - 60 * k)
                bb = int(160 - 100 * k)

            img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({size}x{size})")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--size",
        type=int,
        default=DEFAULT_SIZE,
        help=f"output image size in pixels (default: {DEFAULT_SIZE})",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("assets") / "effects" / "clash.png",
        help="output PNG path (default: assets/effects/clash.png)",
    )
    args = parser.parse_args()
    generate(args.out, args.size)


if __name__ == "__main__":
    main()
