"""Generate projectile VFX sprites.

Produces:
    assets/skills/sprites/fireball.png  — bright orange-yellow fireball with trailing glow
    assets/skills/sprites/venom_ball.png — sickly green-purple glob with drip texture

Usage:
    python tools/generate_projectile_vfx.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image


ASSETS_DIR = Path("assets") / "skills" / "sprites"
SIZE = 128


def generate_fireball(out: Path, size: int) -> None:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = size * 0.55  # offset right so trailing glow goes left
    cy = size / 2.0
    max_r = size / 2.0

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            if r > max_r:
                continue
            t = r / max_r

            # Core glow — tight bright center.
            core = max(0.0, (1.0 - t * 2.5)) ** 1.5

            # Outer flame — wider, softer.
            flame = max(0.0, 1.0 - t) ** 2

            # Trailing tail — stretches left.
            tail = 0.0
            if dx < 0:
                tail_t = min(1.0, abs(dx) / max_r)
                vert = max(0.0, 1.0 - abs(dy) / (max_r * 0.3 * (1.0 + tail_t)))
                tail = tail_t * vert * 0.6

            alpha = min(1.0, core + flame * 0.7 + tail)
            if alpha < 0.01:
                continue

            # White core → yellow → orange → dark red at edge.
            if t < 0.2:
                rr, gg, bb = 255, 255, 220
            elif t < 0.45:
                k = (t - 0.2) / 0.25
                rr = 255
                gg = int(255 - 80 * k)
                bb = int(220 - 180 * k)
            elif t < 0.7:
                k = (t - 0.45) / 0.25
                rr = 255
                gg = int(175 - 95 * k)
                bb = int(40 - 20 * k)
            else:
                k = (t - 0.7) / 0.3
                rr = int(255 - 60 * k)
                gg = int(80 - 50 * k)
                bb = int(20)

            img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({size}x{size})")


def generate_venom_ball(out: Path, size: int) -> None:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = cy = size / 2.0
    max_r = size * 0.42  # slightly smaller than full — room for drip

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)

            # Main glob — slightly squashed vertically to look blobby.
            blob_r = math.sqrt(dx * dx + (dy * 1.2) ** 2)
            if blob_r <= max_r:
                t = blob_r / max_r

                # Bumpy surface: angular distortion.
                angle = math.atan2(dy, dx)
                bump = 0.08 * math.sin(angle * 7) + 0.05 * math.sin(angle * 13)
                t = min(1.0, max(0.0, t + bump))

                alpha = max(0.0, (1.0 - t) ** 1.2)

                # Dark purple core → toxic green → dark edge.
                if t < 0.3:
                    k = t / 0.3
                    rr = int(80 + 20 * k)
                    gg = int(40 + 100 * k)
                    bb = int(120 - 40 * k)
                elif t < 0.65:
                    k = (t - 0.3) / 0.35
                    rr = int(100 - 50 * k)
                    gg = int(140 + 80 * k)
                    bb = int(80 - 40 * k)
                else:
                    k = (t - 0.65) / 0.35
                    rr = int(50 - 30 * k)
                    gg = int(220 - 100 * k)
                    bb = int(40 + 20 * k)

                img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))
                continue

            # Drip below the glob.
            drip_cx = cx
            drip_top = cy + max_r * 0.6
            if y > drip_top and abs(x - drip_cx) < max_r * 0.15:
                drip_len = max_r * 0.7
                drip_dy = y - drip_top
                if drip_dy < drip_len:
                    dt = drip_dy / drip_len
                    width_fade = 1.0 - abs(x - drip_cx) / (max_r * 0.15)
                    alpha = (1.0 - dt) ** 2 * width_fade * 0.7
                    rr, gg, bb = 50, 180, 60
                    img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({size}x{size})")


def generate_seed_ball(out: Path, size: int) -> None:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = cy = size / 2.0
    max_r = size * 0.42

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy

            # Seed body — slightly egg-shaped (taller than wide).
            body_r = math.sqrt((dx * 1.15) ** 2 + dy * dy)
            if body_r <= max_r:
                t = body_r / max_r

                # Husk ridges: angular distortion, fewer/broader than venom's bumps.
                angle = math.atan2(dy, dx)
                ridge = 0.06 * math.sin(angle * 5)
                t = min(1.0, max(0.0, t + ridge))

                alpha = max(0.0, (1.0 - t) ** 1.2)

                # Pale sprout-green core → leaf green → brown husk edge.
                if t < 0.3:
                    k = t / 0.3
                    rr = int(190 - 80 * k)
                    gg = int(230 - 40 * k)
                    bb = int(140 - 60 * k)
                elif t < 0.65:
                    k = (t - 0.3) / 0.35
                    rr = int(110 - 30 * k)
                    gg = int(190 - 60 * k)
                    bb = int(80 - 30 * k)
                else:
                    k = (t - 0.65) / 0.35
                    rr = int(80 + 30 * k)
                    gg = int(130 - 60 * k)
                    bb = int(50 - 20 * k)

                img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))
                continue

            # Trailing leaf-mote flecks above the seed (it tumbles as it flies).
            fleck_top = cy - max_r * 1.15
            if y < cy - max_r * 0.5 and y > fleck_top:
                wave = math.sin(y * 0.35) * max_r * 0.25
                if abs(x - (cx + wave)) < max_r * 0.08:
                    ft = (cy - max_r * 0.5 - y) / (max_r * 0.65)
                    alpha = max(0.0, (1.0 - ft)) ** 2 * 0.5
                    img.putpixel((x, y), (140, 210, 90, int(255 * alpha)))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({size}x{size})")


def generate_void_orb(out: Path, size: int) -> None:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx = cy = size / 2.0
    max_r = size * 0.46

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            if r > max_r:
                continue
            t = r / max_r

            # Swirling rim: angular distortion that tightens toward the edge.
            angle = math.atan2(dy, dx)
            swirl = 0.07 * math.sin(angle * 3 + t * 6)
            t = min(1.0, max(0.0, t + swirl))

            alpha = max(0.0, (1.0 - t) ** 0.8)

            # Near-black core → deep violet → bright magenta rim glow.
            if t < 0.4:
                k = t / 0.4
                rr = int(15 + 35 * k)
                gg = int(5 + 10 * k)
                bb = int(30 + 60 * k)
            elif t < 0.75:
                k = (t - 0.4) / 0.35
                rr = int(50 + 110 * k)
                gg = int(15 + 25 * k)
                bb = int(90 + 110 * k)
            else:
                k = (t - 0.75) / 0.25
                rr = int(160 + 60 * k)
                gg = int(40 + 60 * k)
                bb = int(200 + 40 * k)
                alpha *= (1.0 - k * 0.5)  # rim glow feathers out

            img.putpixel((x, y), (rr, gg, bb, int(255 * alpha)))

    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG")
    print(f"Wrote {out} ({size}x{size})")


def main() -> None:
    generate_fireball(ASSETS_DIR / "fireball.png", SIZE)
    generate_venom_ball(ASSETS_DIR / "venom_ball.png", SIZE)
    generate_seed_ball(ASSETS_DIR / "seed_ball.png", SIZE)
    generate_void_orb(ASSETS_DIR / "void_orb.png", SIZE)


if __name__ == "__main__":
    main()
