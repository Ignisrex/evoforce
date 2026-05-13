"""Generate character sprite PNGs via the PixelLab REST API.

Mirror of tools/generate_panels_pixellab.py but for entity sprites:
reads PIXELLAB_SECRET, calls POST /v1/generate-image-pixflux once per
sprite entry, writes PNGs to assets/sprites/<name>.png.

Usage:
    source ~/.pixellab.env
    python tools/generate_sprites_pixellab.py
    python tools/generate_sprites_pixellab.py --sprites beastkin --size 128
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

SPRITES: dict[str, str] = {
    "beastkin": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "feral humanoid beast-kin in base evolution form, wolf-like ears and tail, "
        "lean muscular body, sharp claws, fangs bared, tattered cloth wrappings on arms, "
        "glowing amber eyes, dark fur with faint bioluminescent markings, "
        "crouched battle-ready stance, feet visible at bottom of frame, "
        "game character sprite, single idle frame, centered, transparent background"
    ),
    "skeleton": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "armored skeleton warrior enemy, ancient cracked plate armor with worn engravings, "
        "rusted sword raised, battered shield, hollow eye sockets with cold blue-green flame, "
        "crumbling bone joints visible between armor plates, "
        "aggressive battle stance, feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),
}

NEGATIVE = (
    "text, logos, watermark, border frame, background scenery, floor shadow, "
    "multiple frames, animation strip, spritesheet, "
    "3d render, photograph, blurry, low detail"
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


def generate_sprite(session: requests.Session, name: str, description: str,
                    size: int, out_path: Path) -> None:
    payload = {
        "description": description,
        "negative_description": NEGATIVE,
        "image_size": {"width": size, "height": size},
        "no_background": True,
        "text_guidance_scale": 9.0,
        "outline": "single color black outline",
        "shading": "highly detailed shading",
        "detail": "highly detailed",
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
        "--sprites",
        nargs="*",
        choices=sorted(SPRITES.keys()),
        help="Subset of sprites to generate (default: all)",
    )
    parser.add_argument(
        "--size",
        type=int,
        default=128,
        help="Square sprite size in pixels (default: 128)",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("assets") / "sprites",
        help="Output directory (default: assets/sprites)",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=0.5,
        help="Seconds to sleep between API calls (default: 0.5)",
    )
    args = parser.parse_args()

    token = require_token()
    selected = args.sprites or list(SPRITES.keys())
    print(f"Generating {len(selected)} sprite(s) at {args.size}x{args.size}...")

    session = requests.Session()
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    })

    for i, name in enumerate(selected):
        out = args.out_dir / f"{name}.png"
        print(f"[{i+1}/{len(selected)}] {name}")
        generate_sprite(session, name, SPRITES[name], args.size, out)
        if i < len(selected) - 1:
            time.sleep(args.sleep)

    print("Done.")


if __name__ == "__main__":
    main()
