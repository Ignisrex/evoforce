"""Generate character sprite PNGs via the PixelLab REST API.

NOTE: The current enemy/creature sprites are generated via the PixelLab MCP
(create_character / create_character_state, Pro mode, 8 directions), not this
script. See tools/pixellab_characters.md for the manifest (character IDs,
lineage group_ids, and the SE=player / SW=enemy orientation convention). This
REST script produces single painterly billboards and is kept for reference and
quick one-offs only.

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

    # ── Undead caster evolution line: lich -> elder_lich -> lich_king ──
    # Each shares the gaunt-undead-sorcerer silhouette and violet arcane flame,
    # escalating in ornamentation and presence so the lineage reads.
    "lich": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "gaunt undead sorcerer lich, skeletal humanoid wrapped in tattered dark robes, "
        "withered grey skin stretched over bone, glowing violet eyes set in a hollow skull, "
        "bony fingers wreathed in crackling purple arcane flame, clutching a worn bone staff, "
        "faint necrotic energy aura, menacing spellcaster stance, robe hem and feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),
    "elder_lich": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "elder lich, a more powerful evolved form of the common lich, taller and more imposing, "
        "ornate layered necromancer robes trimmed with tarnished gold and dangling bone fetishes, "
        "a cracked crown of jagged horns, intense violet-and-cyan dual-flame eyes in a skull face, "
        "swirling arcane runes orbiting both raised hands, frayed flowing cape, "
        "commanding archmage stance, robe hem and feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),
    "lich_king": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "lich king, the regal apex evolution of the elder lich, towering skeletal monarch, "
        "heavy ornate black-and-gold spaulders and armor layered over flowing dark robes, "
        "a massive jagged iron crown, skull face with blazing blue soul-fire eyes, "
        "wielding a great runed scepter, cold necromantic aura radiating power, "
        "imperious throne-king stance, robe hem and feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),

    # ── Beastkin evolutions (share the base beastkin's amber eyes, dark fur with
    # faint bioluminescent markings, and tattered cloth arm wrappings) ──
    "lycan": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "lycan, an evolved beastkin pushed toward a hulking canine werewolf form, "
        "massively muscular wolf-beast, elongated wolf muzzle, large pointed ears, bristling dark fur, "
        "prominent fangs and long claws, tattered cloth wrappings on the arms, glowing amber eyes, "
        "faint bioluminescent markings on the fur, hunched feral predator stance, feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),
    "lionen": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "lionen, an evolved beastkin pushed toward a powerful feline lion-beast form, "
        "broad-shouldered big-cat warrior, feline face framed by a thick mane of fur, rounded ears, "
        "heavy paws with retractable claws, tawny fur with faint bioluminescent markings, glowing amber eyes, "
        "tattered cloth wrappings on the arms, proud battle stance, feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),
    "undead_beastkin": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "undead beastkin, the skeletal undead form of a beastkin, exposed bleached bone fused with "
        "patches of decaying dark fur, a wolf-like skull with pointed ears, hollow eye sockets lit by "
        "faint amber soul-flame, bony clawed hands, tattered cloth arm wrappings, gaunt feral skeletal frame, "
        "crouched battle-ready stance matching the living beastkin, feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),

    # ── Mythical apex evolutions ──
    "fenrir": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "Fenrir, a mythical apex canine evolution of the lycan line, colossal majestic wolf-beast "
        "with thick luminous white fur, powerful lupine build, piercing icy-blue eyes, frost-touched "
        "fangs and claws, faint pale-blue glowing runic markings across the fur, a regal yet ferocious "
        "primal stance, feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),
    "nemean": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "the Nemean, a mythical apex feline evolution of the lionen line based on the heraldic Nemean lion, "
        "regal lion-beast in a proud heraldic rampant pose, golden impenetrable mane and coat with a "
        "burnished metallic sheen, blazing amber eyes, gilded ornamental crest markings, noble silhouette, "
        "feet visible at bottom of frame, "
        "dungeon enemy game sprite, single idle frame, centered, transparent background"
    ),
    "eclipse_beast": (
        "HD-2D pixel art, three-quarter perspective view from slightly above, "
        "the Eclipse Beast, a convergent mythical evolution merging the white-furred Fenrir wolf and the "
        "golden heraldic Nemean lion, a chimeric apex beast with a half-lupine half-feline silhouette, fur "
        "split between luminous white and burnished gold, a dark eclipse-sun corona radiating behind it, "
        "heterochromatic icy-blue and molten-amber eyes, frost-and-radiant energy markings, awe-inspiring "
        "legendary stance, feet visible at bottom of frame, "
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
