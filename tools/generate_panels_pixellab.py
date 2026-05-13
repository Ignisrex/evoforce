"""Generate themed battlefield panel PNGs via the PixelLab REST API.

Reads the API token from the PIXELLAB_SECRET environment variable, calls
POST /v1/generate-image-pixflux for each theme, and writes the decoded
PNG to assets/panels/<theme>.png.

Usage:
    source ~/.pixellab.env           # (or otherwise export PIXELLAB_SECRET)
    python tools/generate_panels_pixellab.py
    python tools/generate_panels_pixellab.py --themes normal_blue cracked ice
    python tools/generate_panels_pixellab.py --size 96
"""

from __future__ import annotations

import argparse
import base64
import os
import sys
import time
from pathlib import Path

import requests


API_BASE = "https://api.pixellab.ai/v1"
ENDPOINT = f"{API_BASE}/generate-image-pixflux"

# description is a PixelLab Pixflux prompt. Keep subject tight and style
# consistent across themes so the 18-panel grid feels cohesive.
THEMES: dict[str, str] = {
    "normal_blue": (
        "top-down square battle arena floor panel, clean metallic blue, "
        "glowing cyan edges, Mega Man Battle Network style, isolated tile, "
        "crisp pixel art"
    ),
    "normal_red": (
        "top-down square battle arena floor panel, clean metallic red, "
        "glowing orange edges, Mega Man Battle Network style, isolated tile, "
        "crisp pixel art"
    ),
    "cracked": (
        "top-down square battle arena floor panel, dark metal with visible "
        "cracks and fractures across the surface, Mega Man Battle Network "
        "style, isolated tile, crisp pixel art"
    ),
    "broken": (
        "top-down square battle arena floor panel, broken with a jagged hole "
        "in the center, dark void visible beneath, Mega Man Battle Network "
        "style, isolated tile, crisp pixel art"
    ),
    "ice": (
        "top-down square battle arena floor panel, frozen pale-blue ice "
        "surface with frost patterns, Mega Man Battle Network style, "
        "isolated tile, crisp pixel art"
    ),
    "lava": (
        "top-down square battle arena floor panel, glowing molten lava with "
        "orange and yellow cracks, dark basalt border, Mega Man Battle "
        "Network style, isolated tile, crisp pixel art"
    ),
    "grass": (
        "top-down square battle arena floor panel, lush green grass with "
        "small flowers, soil border, Mega Man Battle Network style, "
        "isolated tile, crisp pixel art"
    ),
    "poison": (
        "top-down square battle arena floor panel, bubbling toxic purple "
        "swamp surface, Mega Man Battle Network style, isolated tile, "
        "crisp pixel art"
    ),
}

NEGATIVE = (
    "characters, people, monsters, text, logos, border frame, watermark, "
    "shading errors, 3d render, photograph"
)


def require_token() -> str:
    token = os.environ.get("PIXELLAB_SECRET", "").strip()
    if not token:
        sys.exit(
            "PIXELLAB_SECRET is not set. Run:\n"
            "    source ~/.pixellab.env\n"
            "or export it manually before re-running this script."
        )
    return token


def generate_panel(session: requests.Session, theme: str, description: str,
                   size: int, out_path: Path) -> None:
    payload = {
        "description": description,
        "negative_description": NEGATIVE,
        "image_size": {"width": size, "height": size},
        "no_background": True,
        "text_guidance_scale": 8.0,
        "outline": "single color black outline",
        "shading": "basic shading",
        "detail": "medium detail",
    }
    r = session.post(ENDPOINT, json=payload, timeout=120)
    if r.status_code != 200:
        raise SystemExit(
            f"[{theme}] PixelLab API {r.status_code}: {r.text[:500]}"
        )
    body = r.json()
    # Response shape: {"image": {"type": "base64", "base64": "..."}, "usage": {...}}
    image = body.get("image") or {}
    b64 = image.get("base64") or image.get("data")
    if not b64:
        raise SystemExit(
            f"[{theme}] Unexpected response, no image.base64: {body}"
        )
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(base64.b64decode(b64))
    print(f"  wrote {out_path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--themes",
        nargs="*",
        choices=sorted(THEMES.keys()),
        help="Subset of themes to generate (default: all)",
    )
    parser.add_argument(
        "--size",
        type=int,
        default=64,
        help="Square panel size in pixels (default: 64)",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("assets") / "panels",
        help="Output directory (default: assets/panels)",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=0.5,
        help="Seconds to sleep between API calls (default: 0.5)",
    )
    args = parser.parse_args()

    token = require_token()
    selected = args.themes or list(THEMES.keys())
    print(f"Generating {len(selected)} panel(s) at {args.size}x{args.size}...")

    session = requests.Session()
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    })

    for i, theme in enumerate(selected):
        out = args.out_dir / f"{theme}.png"
        print(f"[{i+1}/{len(selected)}] {theme}")
        generate_panel(session, theme, THEMES[theme], args.size, out)
        if i < len(selected) - 1:
            time.sleep(args.sleep)

    print("Done.")


if __name__ == "__main__":
    main()
