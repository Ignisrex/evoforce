"""Bake the painted stars out of mainmenu_background.png into their own
transparent layer, so menu_backdrop.frag can draw (and twinkle) them itself while
the base painting ships without them. (The lightning is drawn procedurally by the
shader and needs no layer.)

Outputs (assets/ui/):
    menu_stars.png  RGBA, the sky's stars on transparent

Also prints every star it found (uv x, uv y, radius px, peak brightness) so the
positions are on record even if the base painting changes.

Usage:
    python tools/extract_menu_layers.py [--src assets/mainmenu_background.png]
"""
import argparse
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def box(u, v, x0, y0, x1, y1):
    return (u >= x0) & (u <= x1) & (v >= y0) & (v <= y1)


def components(mask):
    """Yield lists of (y, x) pixels for each 8-connected blob in a bool mask."""
    seen = np.zeros_like(mask, dtype=bool)
    H, W = mask.shape
    ys, xs = np.nonzero(mask)
    for sy, sx in zip(ys, xs):
        if seen[sy, sx]:
            continue
        blob, q = [], deque([(sy, sx)])
        seen[sy, sx] = True
        while q:
            y, x = q.popleft()
            blob.append((y, x))
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < H and 0 <= nx < W and mask[ny, nx] and not seen[ny, nx]:
                        seen[ny, nx] = True
                        q.append((ny, nx))
        yield blob


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", default="assets/mainmenu_background.png")
    ap.add_argument("--out", default="assets/ui")
    args = ap.parse_args()

    img = np.asarray(Image.open(args.src).convert("RGB")).astype(np.float32) / 255.0
    H, W, _ = img.shape
    r, g, b = img[..., 0], img[..., 1], img[..., 2]
    lum = (r + g + b) / 3.0
    sat = img.max(-1) - img.min(-1)
    yy, xx = np.mgrid[0:H, 0:W]
    u, v = xx / W, yy / H

    # ---- stars: bright, pale points in the open night sky. The citadel spire and
    # its halo rings, the ice spires and the storm are all kept out by geometry.
    sky = box(u, v, 0.56, 0.00, 0.82, 0.19) | box(u, v, 0.82, 0.00, 1.00, 0.16)   # below 0.19 the ice spires start
    spire = box(u, v, 0.44, 0.00, 0.60, 0.26)
    star_alpha = smoothstep(0.55, 0.80, lum) * smoothstep(0.40, 0.18, sat) * sky * ~spire
    star_alpha[star_alpha < 0.05] = 0.0

    stars = np.zeros((H, W, 4), np.float32)
    stars[..., :3] = img
    stars[..., 3] = star_alpha
    Image.fromarray((stars * 255).astype(np.uint8), "RGBA").save(Path(args.out) / "menu_stars.png")

    found = []
    for blob in components(star_alpha > 0.15):
        ys = np.array([p[0] for p in blob]); xs = np.array([p[1] for p in blob])
        peak = float(lum[ys, xs].max())
        found.append((xs.mean() / W, ys.mean() / H, max(1.0, np.sqrt(len(blob) / np.pi)), peak))
    found.sort(key=lambda s: (-s[3], s[0]))
    print(f"stars: {len(found)}  (uv x, uv y, radius px, peak lum)")
    for s in found:
        print(f"  {s[0]:.4f} {s[1]:.4f}  r={s[2]:.1f}  lum={s[3]:.2f}")


if __name__ == "__main__":
    main()
