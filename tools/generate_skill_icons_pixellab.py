"""Generate skill icon PNGs via the PixelLab REST API.

Sibling to generate_attacks_pixellab.py / generate_sprites_pixellab.py:
reads PIXELLAB_SECRET, calls POST /v1/generate-image-pixflux once per
entry, writes PNGs to assets/skills/<name>.png.

Existing skill icons in assets/skills/ are 112x96 (slightly wider than
tall, transparent background, MMBN-style); we default to that aspect
ratio so new icons drop into the SlotsHud / hand UI without re-tuning.

Usage:
    source ~/.pixellab.env
    python tools/generate_skill_icons_pixellab.py
    python tools/generate_skill_icons_pixellab.py --icons regen
    python tools/generate_skill_icons_pixellab.py --width 96 --height 96
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

# Per-icon prompts. Keep the style cues consistent so new icons sit visually
# next to heal.png / shield.png / ice_beam.png without looking like outliers.
ICONS: dict[str, str] = {
    "regen": (
        "pixel art skill icon, swirling regeneration symbol, two intertwined "
        "looping arrows forming a circular spiral of emerald green healing "
        "energy, bright lime and teal glow, soft yellow sparkle highlights, "
        "vibrant life-essence aura, Mega Man Battle Network style icon, "
        "centered composition, transparent background"
    ),
}

NEGATIVE = (
    "cross, plus sign, heart, shield, hexagon, character, monster, person, "
    "text, letters, numbers, logos, watermark, border frame, ui chrome, "
    "background scenery, sky, ground, 3d render, photograph, blurry"
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


def generate_icon(session: requests.Session, name: str, description: str,
                  width: int, height: int, out_path: Path) -> None:
    payload = {
        "description": description,
        "negative_description": NEGATIVE,
        "image_size": {"width": width, "height": height},
        "no_background": True,
        "text_guidance_scale": 8.0,
        "outline": "single color black outline",
        "shading": "basic shading",
        "detail": "medium detail",
    }
    r = session.post(ENDPOINT, json=payload, timeout=120)
    if r.status_code != 200:
        raise SystemExit(
            f"[{name}] PixelLab API {r.status_code}: {r.text[:500]}"
        )
    body = r.json()
    image = body.get("image") or {}
    b64 = image.get("base64") or image.get("data")
    if not b64:
        raise SystemExit(
            f"[{name}] Unexpected response, no image.base64: {body}"
        )
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(base64.b64decode(b64))
    print(f"  wrote {out_path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--icons",
        nargs="*",
        choices=sorted(ICONS.keys()),
        help="Subset of icons to generate (default: all defined)",
    )
    parser.add_argument(
        "--width",
        type=int,
        default=112,
        help="Icon width in pixels (default: 112, matches existing icons)",
    )
    parser.add_argument(
        "--height",
        type=int,
        default=96,
        help="Icon height in pixels (default: 96, matches existing icons)",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("assets") / "skills",
        help="Output directory (default: assets/skills)",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=0.5,
        help="Seconds to sleep between API calls (default: 0.5)",
    )
    args = parser.parse_args()

    token = require_token()
    selected = args.icons or list(ICONS.keys())
    print(f"Generating {len(selected)} icon(s) at {args.width}x{args.height}...")

    session = requests.Session()
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    })

    for i, name in enumerate(selected):
        out = args.out_dir / f"{name}.png"
        print(f"[{i+1}/{len(selected)}] {name}")
        generate_icon(session, name, ICONS[name], args.width, args.height, out)
        if i < len(selected) - 1:
            time.sleep(args.sleep)

    print("Done.")


if __name__ == "__main__":
    main()
