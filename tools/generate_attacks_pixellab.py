"""Generate attack / projectile sprite PNGs via the PixelLab REST API.

Mirror of tools/generate_sprites_pixellab.py but for attack effects:
reads PIXELLAB_SECRET, calls POST /v1/generate-image-pixflux once per
attack entry, writes PNGs to assets/attacks/<name>.png.

Usage:
    source ~/.pixellab.env
    python tools/generate_attacks_pixellab.py
    python tools/generate_attacks_pixellab.py --attacks wind_slash --size 64
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

ATTACKS: dict[str, str] = {
    "wind_slash": (
        "pixel art horizontal crescent slash attack effect, sharp white "
        "cutting arc, bright clean sword slash mark, fast-moving projectile, "
        "Mega Man Battle Network style, centered, transparent background"
    ),
}

NEGATIVE = (
    "wind swirls, tornado, air currents, clouds, storm, character, monster, "
    "text, logos, watermark, border frame, background scenery, "
    "3d render, photograph, blurry"
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


def generate_attack(session: requests.Session, name: str, description: str,
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
        "--attacks",
        nargs="*",
        choices=sorted(ATTACKS.keys()),
        help="Subset of attacks to generate (default: all)",
    )
    parser.add_argument(
        "--size",
        type=int,
        default=64,
        help="Square sprite size in pixels (default: 64)",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("assets") / "attacks",
        help="Output directory (default: assets/attacks)",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=0.5,
        help="Seconds to sleep between API calls (default: 0.5)",
    )
    args = parser.parse_args()

    token = require_token()
    selected = args.attacks or list(ATTACKS.keys())
    print(f"Generating {len(selected)} attack(s) at {args.size}x{args.size}...")

    session = requests.Session()
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    })

    for i, name in enumerate(selected):
        out = args.out_dir / f"{name}.png"
        print(f"[{i+1}/{len(selected)}] {name}")
        generate_attack(session, name, ATTACKS[name], args.size, out)
        if i < len(selected) - 1:
            time.sleep(args.sleep)

    print("Done.")


if __name__ == "__main__":
    main()
