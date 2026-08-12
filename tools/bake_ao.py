"""Bake an ambient-occlusion map into a diffuse texture for the cave surfaces.

The stock libGDX DefaultShader only reliably uses diffuse maps, so surface
shading detail is multiplied in offline. Idempotent: pure function of inputs.

Sources (CC0, ambientcg.com): Rock035 (wall), Rock030 (floor).

Usage:
    python tools/bake_ao.py --diffuse tools/textures_raw/Rock035_1K-JPG/Rock035_1K-JPG_Color.jpg --ao tools/textures_raw/Rock035_1K-JPG/Rock035_1K-JPG_AmbientOcclusion.jpg --out assets/cave_wall.png --strength 0.4 --gain 1.9
    python tools/bake_ao.py --diffuse tools/textures_raw/Rock030_1K-JPG/Rock030_1K-JPG_Color.jpg --ao tools/textures_raw/Rock030_1K-JPG/Rock030_1K-JPG_AmbientOcclusion.jpg --out assets/cave_floor.png --gain 0.75
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageEnhance


def bake(diffuse: Path, ao: Path, out: Path, strength: float, size: int, gain: float) -> None:
    color = Image.open(diffuse).convert("RGB")
    occ = Image.open(ao).convert("L").resize(color.size)
    if strength < 1.0:
        white = Image.new("L", occ.size, 255)
        occ = Image.blend(white, occ, strength)
    baked = ImageChops.multiply(color, Image.merge("RGB", (occ, occ, occ)))
    if gain != 1.0:
        baked = ImageEnhance.Brightness(baked).enhance(gain)
    baked = baked.resize((size, size), Image.LANCZOS)
    baked.save(out)
    print(f"wrote {out} ({size}x{size})")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--diffuse", type=Path, required=True)
    p.add_argument("--ao", type=Path, required=True)
    p.add_argument("--out", type=Path, required=True)
    p.add_argument("--strength", type=float, default=1.0)
    p.add_argument("--size", type=int, default=1024)
    p.add_argument("--gain", type=float, default=1.0)
    args = p.parse_args()
    bake(args.diffuse, args.ao, args.out, args.strength, args.size, args.gain)


if __name__ == "__main__":
    main()
