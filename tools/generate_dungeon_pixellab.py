"""Generate cave environment assets via the PixelLab REST API.

Generates three layered assets that work together in PlayState.renderWorld():

  assets/cave_wall.png        — full-screen cave backdrop (wall + ceiling)
  assets/cave_floor.png       — floor detail drawn at the grid footprint
  assets/cave_foreground.png  — foreground rocks drawn OVER entities for depth

World-space layout (16x9 viewport):
  - Grid occupies x=[3,13], y=[1,3.4]  (10 wide × 2.4 tall compressed)
  - cave_wall.png rendered full-screen (0,0,16,9)
  - cave_floor.png rendered at grid position (3,1,10,2.4)
  - cave_foreground.png rendered at bottom (0,0,16,1.5), transparent PNG

Usage:
    source ~/.pixellab.env
    python tools/generate_dungeon_pixellab.py
    python tools/generate_dungeon_pixellab.py --assets cave_wall
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

ASSETS: dict[str, dict] = {
    "cave_wall": {
        "out": Path("assets") / "cave_wall.png",
        # 16:9 ratio to match the full viewport
        "size": {"width": 400, "height": 225},
        "no_background": False,
        "description": (
            "HD-2D pixel art monster cave interior, wide panoramic view, "
            "slightly elevated three-quarter perspective angle looking into the cave, "
            "jagged dark rock ceiling with long stalactites hanging down, "
            "rough cave back wall with cracks and embedded glowing crystals, "
            "clusters of bioluminescent blue and purple mushrooms growing on rocks, "
            "faint green and teal cave moss, small pockets of glowing crystal formations, "
            "deep atmospheric darkness at edges, dramatic chiaroscuro lighting "
            "from the glowing crystals and mushrooms, "
            "NO floor visible — the lower 30 percent of the image is black darkness, "
            "empty monster lair, no characters, treasure chests partially visible in corners, "
            "bone fragments scattered, ancient cave atmosphere"
        ),
        "negative": (
            "humans, characters, enemies, text, logos, watermark, "
            "bright daylight, outdoor, stone brick walls, torches, "
            "man-made structures, modern elements, 3d render, photograph, "
            "flat floor, visible ground"
        ),
        "shading": "highly detailed shading",
        "detail": "highly detailed",
        "outline": "lineless",
    },
    "cave_floor": {
        "out": Path("assets") / "cave_floor.png",
        # 4.17:1 ratio to closely match the grid footprint (10 / 2.4)
        "size": {"width": 400, "height": 96},
        "no_background": False,
        "description": (
            "HD-2D pixel art cave rock floor, wide horizontal strip, "
            "top-down view from slightly elevated angle, "
            "damp dark stone and compacted earth, "
            "thin glowing crystal veins running through the rock in blue and teal, "
            "shallow puddles of water reflecting faint crystal light, "
            "patches of luminescent cave moss in corners, "
            "uneven rocky texture with small stones and pebbles, "
            "dark and atmospheric, monster lair floor, "
            "seamless horizontal texture, no walls, no ceiling, only floor"
        ),
        "negative": (
            "walls, ceiling, stalactites, characters, sky, "
            "bright colors, modern elements, text, watermark, 3d render"
        ),
        "shading": "highly detailed shading",
        "detail": "highly detailed",
        "outline": "lineless",
    },
    "cave_foreground": {
        "out": Path("assets") / "cave_foreground.png",
        # 4:1 ratio for a wide foreground strip
        "size": {"width": 400, "height": 100},
        "no_background": True,
        "description": (
            "HD-2D pixel art cave rock foreground silhouette, wide horizontal strip, "
            "dark jagged rock formations and stone ledge at the very bottom of the frame, "
            "rocks rise up from the bottom edge with uneven sharp peaks, "
            "small glowing crystal clusters embedded in the rocks, "
            "bone fragments and pebbles on the rock surface, "
            "the upper half of the image is completely empty transparent space, "
            "only the lower rock formations are solid, "
            "dark silhouette style with subtle detail, "
            "transparent background above the rocks"
        ),
        "negative": (
            "characters, ceiling, stalactites at top, sky, background scenery, "
            "full scene, flat ground, bright colors, text, watermark"
        ),
        "shading": "basic shading",
        "detail": "highly detailed",
        "outline": "single color black outline",
    },
}

SHARED_NEGATIVE = "blurry, low quality"


def require_token() -> str:
    token = os.environ.get("PIXELLAB_SECRET", "").strip()
    if not token:
        sys.exit(
            "PIXELLAB_SECRET is not set. Run:\n"
            "    source ~/.pixellab.env\n"
            "or export it manually before re-running this script."
        )
    return token


def generate_asset(session: requests.Session, name: str, cfg: dict) -> None:
    payload = {
        "description": cfg["description"],
        "negative_description": cfg["negative"] + ", " + SHARED_NEGATIVE,
        "image_size": cfg["size"],
        "no_background": cfg["no_background"],
        "text_guidance_scale": 9.0,
        "outline": cfg["outline"],
        "shading": cfg["shading"],
        "detail": cfg["detail"],
    }
    r = session.post(ENDPOINT, json=payload, timeout=120)
    if r.status_code != 200:
        raise SystemExit(f"[{name}] PixelLab API {r.status_code}: {r.text[:500]}")
    body = r.json()
    image = body.get("image") or {}
    b64 = image.get("base64") or image.get("data")
    if not b64:
        raise SystemExit(f"[{name}] Unexpected response — no image.base64: {body}")
    out: Path = cfg["out"]
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(base64.b64decode(b64))
    print(f"  wrote {out}  ({cfg['size']['width']}x{cfg['size']['height']})")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--assets",
        nargs="*",
        choices=sorted(ASSETS.keys()),
        help="Subset of assets to generate (default: all)",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=1.0,
        help="Seconds between API calls (default: 1.0)",
    )
    args = parser.parse_args()

    token = require_token()
    selected = args.assets or list(ASSETS.keys())
    print(f"Generating {len(selected)} cave asset(s)...")

    session = requests.Session()
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    })

    for i, name in enumerate(selected):
        print(f"[{i+1}/{len(selected)}] {name}")
        generate_asset(session, name, ASSETS[name])
        if i < len(selected) - 1:
            time.sleep(args.sleep)

    print("Done.")


if __name__ == "__main__":
    main()
