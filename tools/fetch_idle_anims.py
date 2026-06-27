"""Download PixelLab breathing-idle frames into tools/pixellab_raw/.

The monsters were animated via the PixelLab MCP (animate_character, v3 mode,
south-east + south-west). Each (monster, direction) animation lives at a public
Backblaze URL keyed by its own animation_id:

    {BASE}/{character_id}/animations/{animation_id}/{direction}/{N}.png

Frame layout is NOT uniform across monsters:
  - v3 6-frame anims ship 7 files (0..6): frame 0 is the static reference
    (byte-identical to the rotation sprite) and 1..6 are the animated cycle.
  - shorter/template anims may ship fewer files, and frame 0 may or may not be
    a reference copy.

So this script PROBES how many frames exist, then drops frame 0 only when it is
byte-identical to that direction's rotation sprite (the reference). Whatever
remains is the animated cycle, written 0-indexed into:

    tools/pixellab_raw/<monster>/idle/<se|sw>/<0..k>.png

build_anim_sheets.py then stitches them as the IDLE row. The per-monster frame
count it produces is printed at the end -- use it to set the IDLE frameCount in
Monster.java.

Animation IDs change on every re-roll, so they are read from
tools/idle_manifest.json rather than hard-coded. Schema:

    { "fenrir": {"character_id": "...", "se": "<anim_id>", "sw": "<anim_id>"}, ... }

Usage:
    python tools/fetch_idle_anims.py                 # every monster in the manifest
    python tools/fetch_idle_anims.py fenrir lycan    # just these
"""

from __future__ import annotations

import json
import shutil
import sys
import urllib.request
from pathlib import Path

BASE = ("https://backblaze.pixellab.ai/file/pixellab-characters/"
        "c688be2f-c859-4ab6-8aee-da6f5fa4cabd")
MANIFEST = Path("tools") / "idle_manifest.json"
RAW_DIR = Path("tools") / "pixellab_raw"

DIR_FULL = {"se": "south-east", "sw": "south-west"}
MAX_PROBE = 16

# Backblaze 403s urllib's default User-Agent; pose as a browser.
_OPENER = urllib.request.build_opener()
_OPENER.addheaders = [("User-Agent", "Mozilla/5.0")]


def _get(url: str) -> bytes | None:
    try:
        with _OPENER.open(url) as r:
            return r.read()
    except Exception:
        return None


def fetch_direction(character_id: str, animation_id: str, monster: str, short: str) -> int:
    full = DIR_FULL[short]

    # Probe frames 0..MAX-1.
    frames: list[bytes] = []
    for n in range(MAX_PROBE):
        data = _get(f"{BASE}/{character_id}/animations/{animation_id}/{full}/{n}.png")
        if data is None:
            break
        frames.append(data)
    if not frames:
        print(f"  {monster}/{short}: NO FRAMES (check animation_id) ")
        return 0

    # PixelLab anims are "1 reference + N animated"; frame 0 is the static
    # reference seed (the rotation pose), not part of the cycle -- always drop it.
    if len(frames) > 1:
        frames = frames[1:]

    out_dir = RAW_DIR / monster / "idle" / short
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    for i, data in enumerate(frames):
        (out_dir / f"{i}.png").write_bytes(data)
    print(f"  {monster}/{short}: {len(frames)} frames -> {out_dir}")
    return len(frames)


def main() -> None:
    manifest = json.loads(MANIFEST.read_text())
    wanted = sys.argv[1:] or sorted(manifest)
    counts: dict[str, dict[str, int]] = {}
    for monster in wanted:
        entry = manifest.get(monster)
        if not entry:
            print(f"{monster}: not in manifest, skipping")
            continue
        print(monster)
        counts[monster] = {}
        for short in ("se", "sw"):
            anim_id = entry.get(short)
            if not anim_id:
                print(f"  {monster}/{short}: no animation_id in manifest")
                continue
            counts[monster][short] = fetch_direction(entry["character_id"], anim_id, monster, short)

    print("\nFrame counts (set Monster.java IDLE frameCount to the se/sw value):")
    for monster, c in counts.items():
        print(f"  {monster:18} se={c.get('se', 0)}  sw={c.get('sw', 0)}")


if __name__ == "__main__":
    main()
