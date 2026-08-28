"""Generate placeholder VFX sprites for each skill shape.

Produces:
    assets/skills/sprites/slash.png  — directional slash arc (Strike)
    assets/skills/sprites/beam.png   — horizontal gradient bar (Beam)
    assets/skills/sprites/aura.png   — radial glow ring (Aura)
    assets/skills/sprites/zone.png   — edge-inward tile border glow (Zone)

Usage:
    python tools/generate_skill_vfx.py
    python tools/generate_skill_vfx.py --size 128
"""

from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image


DEFAULT_SIZE = 128
ASSETS_DIR = Path("assets") / "skills" / "sprites"


def generate_slash(out: Path, size: int) -> None:
    """A diagonal slash arc — bright white core fading to cyan."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = cy = size / 2.0
    max_r = size / 2.0

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            if r > max_r or r < max_r * 0.25:
                continue
            t = r / max_r

            angle = math.atan2(dy, dx)
            # Arc spans roughly -45° to +45° (upper-right diagonal slash).
            arc = max(0.0, math.cos(angle - math.radians(-30)))
            arc = arc ** 4  # tighten the arc
            if arc < 0.05:
                continue

            radial = 1.0 - abs(t - 0.55) / 0.45  # peak brightness at ~55% radius
            radial = max(0.0, radial)
            alpha = arc * radial

            # White center → cyan edge.
            k = t
            rr = int(200 + 55 * (1 - k))
            gg = 255
            bb = 255
            img.putpixel((x, y), (rr, gg, bb, int(255 * min(1.0, alpha))))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({size}x{size})")


def generate_beam(out: Path, size: int) -> None:
    """A horizontal energy bar — bright core with soft vertical falloff."""
    w = size * 2
    h = size
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cy = h / 2.0

    for y in range(h):
        for x in range(w):
            # Vertical falloff from center.
            dy = abs(y - cy) / cy
            v_alpha = max(0.0, 1.0 - dy ** 1.5)

            # Horizontal: bright at left, slight taper at right.
            hx = x / float(w)
            h_alpha = 1.0 - hx * 0.3

            alpha = v_alpha * h_alpha
            if alpha < 0.01:
                continue

            # Color: white core → orange at vertical edges.
            if dy < 0.3:
                rr, gg, bb = 255, 255, 240
            elif dy < 0.6:
                k = (dy - 0.3) / 0.3
                rr = 255
                gg = int(255 - 80 * k)
                bb = int(240 - 140 * k)
            else:
                k = (dy - 0.6) / 0.4
                rr = 255
                gg = int(175 - 75 * k)
                bb = int(100 - 60 * k)

            img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({w}x{h})")


def generate_aura(out: Path, size: int) -> None:
    """A ring glow — transparent center, bright ring, fading outward."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = cy = size / 2.0
    max_r = size / 2.0
    ring_center = 0.6  # ring sits at 60% of radius
    ring_width = 0.25

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            if r > max_r:
                continue
            t = r / max_r

            # Ring-shaped falloff.
            dist_from_ring = abs(t - ring_center) / ring_width
            ring_alpha = max(0.0, 1.0 - dist_from_ring ** 2)

            # Soft inner glow.
            inner = max(0.0, (1.0 - t / ring_center) * 0.25) if t < ring_center else 0.0

            alpha = max(ring_alpha, inner)
            if alpha < 0.01:
                continue

            # Color: green-teal glow.
            k = t
            rr = int(100 + 50 * (1 - k))
            gg = 255
            bb = int(200 + 55 * (1 - k))
            img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({size}x{size})")


def generate_zone(out: Path, size: int) -> None:
    """Edge-inward glow — bright borders fading toward center."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    border = size * 0.25  # glow extends 25% inward from each edge

    for y in range(size):
        for x in range(size):
            # Distance from nearest edge.
            dx = min(x, size - 1 - x)
            dy = min(y, size - 1 - y)
            d = min(dx, dy)

            if d >= border:
                continue

            t = d / border  # 0 at edge, 1 at border depth
            alpha = (1.0 - t) ** 1.5

            # Corner boost: corners are near two edges, so brighten them.
            corner_t = 1.0 - (min(dx, dy) / border if border > 0 else 0)

            # Color: fiery red-orange.
            rr = 255
            gg = int(120 + 80 * t)
            bb = int(40 + 40 * t)
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
        help=f"base image size in pixels (default: {DEFAULT_SIZE})",
    )
    args = parser.parse_args()
    s = args.size

    generate_slash(ASSETS_DIR / "slash.png", s)
    generate_beam(ASSETS_DIR / "beam.png", s)
    generate_aura(ASSETS_DIR / "aura.png", s)
    generate_zone(ASSETS_DIR / "zone.png", s)


if __name__ == "__main__":
    main()
