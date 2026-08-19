"""Generate the main menu's flat-shaded scene as parallax-ready layers.

The menu background is a stylised, simplified version of the concept art in
artReferences/: a stone path climbing out of a crystal cave, past fire, water
terraces and jungle, up to a citadel on an icy summit, with thunder cliffs off
to the right. Clean vector-fantasy — flat fills, crisp silhouettes, one rim
light along each top edge.

Everything static is baked here. Everything that moves (pool glints, waterfall
ribs, lightning, cloud drift, star twinkle) is drawn by
assets/ui/shaders/menu_backdrop.frag, which finds the regions it may animate in
the mask layer rather than repeating any coordinates from this file.

Outputs (assets/ui/menu/):
    sky.png    opaque backdrop: day-to-night gradient plus haze bands
    far.png    citadel, ice shelf, sand mesas
    mid.png    storm cliffs and cloud, fire field, water terraces
    near.png   jungle, path, cave mouth, crystals
    masks.png  R = pool surface, G = waterfall, B = storm rock, A = open sky

Deterministic: pure function of SEED and the layout constants below.

Usage:
    python tools/generate_menu_layers.py
    python tools/generate_menu_layers.py --size 2560 1440 --seed 7
"""

from __future__ import annotations

import argparse
import math
import random
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

SEED = 1337
OUT_DIR = Path("assets/ui/menu")

# ---- palette ---------------------------------------------------------------
SKY_DAY_LOW = (255, 219, 168)
SKY_DAY_HIGH = (115, 173, 235)
SKY_NIGHT_LOW = (26, 33, 87)
SKY_NIGHT_HIGH = (13, 13, 46)
HAZE = (176, 186, 224)

CITADEL_FACE = (158, 194, 250)
CITADEL_RIM = (250, 252, 255)
ICE_FACE = (112, 153, 219)
ICE_RIM = (219, 242, 255)
SAND_FACE = (199, 158, 107)
SAND_RIM = (242, 212, 153)
STORM_FACE = (41, 41, 77)
STORM_RIM = (112, 107, 179)
STORM_CLOUD = (66, 61, 107)
ROCK_FACE = (61, 33, 41)
ROCK_RIM = (110, 62, 66)
LAVA = (255, 112, 31)
WATER_DEEP = (23, 140, 158)
WATER_SHALLOW = (61, 204, 204)
WATER_RIM = (219, 250, 250)
BASIN_FACE = (54, 74, 82)
BASIN_CLIFF = (33, 46, 51)
ICE_CLIFF = (71, 99, 148)
SAND_CLIFF = (135, 105, 71)
STORM_CLIFF = (26, 26, 51)
ROCK_CLIFF = (38, 20, 26)
RIDGE_FACE = (15, 56, 43)
JUNGLE_FACE = (26, 87, 56)
JUNGLE_RIM = (82, 158, 77)
PATH_FACE = (179, 161, 122)
PATH_RIM = (214, 199, 163)
CAVE_ROCK = (11, 11, 22)
CRYSTAL_FACE = (13, 133, 184)
CRYSTAL_RIM = (89, 235, 255)

# ---- layout, in scene units: x 0..1 left to right, y 0..1 bottom to top -----
CITADEL = dict(centre=0.500, half=0.150, base=0.700, pitch=0.019, lo=0.085, hi=0.200)
ICE = dict(centre=0.500, half=0.290, top=0.700, keel=0.070, pitch=0.030, lo=0.010, hi=0.034)
SAND = dict(centre=0.185, half=0.165, top=0.620, keel=0.062, pitch=0.038, lo=0.020, hi=0.070)
STORM = dict(centre=0.855, half=0.190, top=0.560, keel=0.105, pitch=0.026, lo=0.040, hi=0.150)
FIRE = dict(centre=0.150, half=0.185, top=0.430, keel=0.090, pitch=0.030, lo=0.030, hi=0.105)
POOLS = [
    dict(centre=0.420, half=0.115, top=0.575, keel=0.046),
    dict(centre=0.370, half=0.140, top=0.495, keel=0.046),
    dict(centre=0.415, half=0.120, top=0.415, keel=0.046),
]
# Each fall spills from pool i to pool i+1. x is the ribbon's centre.
FALLS = [dict(x=0.500, half=0.018), dict(x=0.320, half=0.020)]
RIDGE = dict(base=0.295, pitch=0.028, lo=0.030, hi=0.075)
JUNGLE = dict(base=0.205, pitch=0.042, lo=0.055, hi=0.130)
PATH = dict(x=0.520, swing=0.075, curve=5.6, top=0.408, near=0.052, far=0.014)
MOUTH = dict(cx=0.500, cy=0.340, rx=0.480, ry=0.950, wobble=0.032)
STORM_CLOUD_BAND = dict(y=0.860, thick=0.075)

DEPTH_FAR = 0.048    # how much of a distant plateau's tabletop the camera sees
DEPTH_MID = 0.036    # the same for the middle distance
FAR_HAZE = 0.34      # how far the distant layer washes toward the sky
MID_HAZE = 0.13      # the same for the middle distance
RIM = 0.011          # thickness of the lit lip along a top edge, in scene units
FACETS = 9           # rock facets across an island's underside
PEAK_JITTER = 0.45   # sideways wander of each peak, in cell widths
PEAK_NARROW = 0.55   # narrowest a peak gets, as a fraction of its cell
PEAK_TAPER = 1.5     # 1 = straight triangle flanks, >1 = sharper crowns
PEAK_GAPS = 0.18     # fraction of cells left flat, so the ridge is not a comb
CANOPY_GAPS = 0.28   # fraction of crowns kept low, so the canopy is not a hedge


# ---- geometry --------------------------------------------------------------

def plateau(cfg: dict, rng: random.Random, depth: float,
            level: bool = False) -> tuple[list, list]:
    """A plateau under a 3/4 downward view: its top face, then its cliff.

    `depth` is how much of the tabletop the camera sees — this is what stops
    the scene reading as flat side-elevation. `level` keeps the far edge
    perfectly straight, which is what a water surface needs.
    """
    cx, half, top, keel = cfg["centre"], cfg["half"], cfg["top"], cfg["keel"]
    steps = 48

    # Parametrised by x so the winding of both edges is unambiguous: the far
    # edge and the near edge both run left to right, and get reversed on use.
    back, near = [], []
    for i in range(steps + 1):
        x = -1.0 + 2.0 * i / steps
        bulge = depth * math.sqrt(max(1.0 - x * x, 0.0))
        broken = 0.0 if level else rng.uniform(0.0, 0.012)
        back.append((cx + x * half, top + bulge - broken))
        near.append((cx + x * half, top - bulge))

    # The keel hangs from an off-centre low point, so the two flanks differ.
    tip = rng.uniform(-0.30, 0.30)
    keel_pts = []
    for i in range(1, 6):                           # right to left, closing the cliff
        x = 1.0 - 2.0 * i / 6.0
        drop = keel * (1.0 - abs(x - tip) / (1.0 + abs(tip))) ** 0.8
        keel_pts.append((cx + (x + rng.uniform(-0.05, 0.05)) * half,
                         top - depth - drop * rng.uniform(0.85, 1.15)))
    return back + near[::-1], near + keel_pts


# Spires stand on the far edge of a plateau, so they read as behind it.
def ridge_base(cfg: dict, depth: float) -> dict:
    return dict(cfg, top=cfg.get("base", cfg.get("top", 0.0)) + depth * 0.55)


def ridge_outline(cfg: dict, rng: random.Random, samples: int = 700) -> list[tuple[float, float]]:
    """A row of spires standing on `base` — cliffs, ice caps, towers.

    Each peak gets its own height, width and sideways nudge, or the row reads
    as a bar chart rather than a skyline.
    """
    base = cfg.get("base", cfg.get("top"))   # spires stand on an island's tabletop
    pitch = cfg["pitch"]
    x0 = cfg["centre"] - cfg["half"]
    x1 = cfg["centre"] + cfg["half"]
    cells = {}

    def cell(i: int) -> tuple[float, float, float]:
        if i not in cells:
            gap = rng.random() < PEAK_GAPS      # a flat shoulder between crowns
            cells[i] = (
                0.0 if gap else rng.uniform(cfg["lo"], cfg["hi"]),
                (rng.random() - 0.5) * PEAK_JITTER,
                rng.uniform(PEAK_NARROW, 1.0),
            )
        return cells[i]

    top = []
    for s in range(samples + 1):
        x = x0 + (x1 - x0) * s / samples
        i = math.floor(x / pitch)
        height, nudge, width = cell(i)
        lx = abs((x / pitch) % 1.0 - 0.5 - nudge) / (width * 0.5)
        top.append((x, base + height * max(1.0 - lx, 0.0) ** PEAK_TAPER))
    return top + [(x1, base), (x0, base)]


def canopy_outline(base: float, pitch: float, lo: float, hi: float,
                   rng: random.Random, samples: int = 900) -> list[tuple[float, float]]:
    """Overlapping rounded crowns standing on `base`."""
    cells = {}

    def height(i: int) -> float:
        if i not in cells:
            low = rng.random() < CANOPY_GAPS      # a dip between clumps of crowns
            cells[i] = rng.uniform(lo * 0.25, lo * 0.6) if low else rng.uniform(lo, hi)
        return cells[i]

    top = []
    for s in range(samples + 1):
        x = -0.05 + 1.10 * s / samples
        i = math.floor(x / pitch)
        lx = ((x / pitch) % 1.0 - 0.5) * 2.0
        top.append((x, base + height(i) * math.sqrt(max(1.0 - lx * lx, 0.0))))
    
    return top + [(1.05, -0.05), (-0.05, -0.05)]


def path_outline(samples: int = 200) -> list[tuple[float, float]]:
    """The stone path: a ribbon that narrows with distance as it climbs."""
    left, right = [], []
    for s in range(samples + 1):
        y = PATH["top"] * s / samples
        centre = PATH["x"] + PATH["swing"] * math.sin(y * PATH["curve"])
        t = y / PATH["top"]
        w = PATH["near"] + (PATH["far"] - PATH["near"]) * (t * t * (3 - 2 * t))
        left.append((centre - w, y))
        right.append((centre + w, y))
    return left + right[::-1]


def mouth_outline(samples: int = 720) -> list[tuple[float, float]]:
    """The cave opening we look out through, as a ragged ellipse."""
    pts = []
    for s in range(samples):
        a = 2.0 * math.pi * s / samples
        r = 1.0 + MOUTH["wobble"] * math.sin(a * 7.0) + MOUTH["wobble"] * 0.5 * math.sin(a * 17.0 + 1.3)
        pts.append((MOUTH["cx"] + math.cos(a) * MOUTH["rx"] * r,
                    MOUTH["cy"] + math.sin(a) * MOUTH["ry"] * r))
    return pts


def shard_outlines(rng: random.Random) -> list[list[tuple[float, float]]]:
    """Crystal shards leaning up out of the bottom corners of the frame."""
    out = []
    for x in [i / 24.0 for i in range(25)]:
        corner = max(0.0, min(1.0, (0.26 - x) / 0.20)), max(0.0, min(1.0, (x - 0.74) / 0.20))
        if max(corner) < 0.15:
            continue
        h = rng.uniform(0.030, 0.105) * max(corner)
        w = rng.uniform(0.006, 0.015)
        lean = rng.uniform(-0.5, 0.5) * w
        out.append([(x - w, 0.0), (x + w, 0.0), (x + lean, h)])
    return out


# ---- drawing ---------------------------------------------------------------

class Canvas:
    """An RGBA layer that takes shapes in scene units (y up)."""

    def __init__(self, size: tuple[int, int]):
        self.w, self.h = size
        self.img = Image.new("RGBA", size, (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.img)

    def px(self, pts):
        return [(x * self.w, (1.0 - y) * self.h) for x, y in pts]

    def shape(self, pts, face, rim=None, rim_size=RIM):
        """Fill a silhouette, leaving a lit lip along its top edge."""
        if rim is not None:
            self.draw.polygon(self.px(pts), fill=rim + (255,))
            pts = [(x, y - rim_size) for x, y in pts]
        self.draw.polygon(self.px(pts), fill=face + (255,))


def vertical_gradient(size, low, high):
    strip = Image.new("RGB", (1, 256))
    for y in range(256):
        t = 1.0 - y / 255.0
        strip.putpixel((0, y), tuple(round(low[i] + (high[i] - low[i]) * t) for i in range(3)))
    return strip.resize(size, Image.BICUBIC)


def build_sky(size) -> Image.Image:
    """Warm daylight on the left rising into cosmic night on the right."""
    w, h = size
    day = vertical_gradient(size, SKY_DAY_LOW, SKY_DAY_HIGH)
    night = vertical_gradient(size, SKY_NIGHT_LOW, SKY_NIGHT_HIGH)

    # Blend along the same diagonal the shader's stars fade on.
    ramp = Image.new("L", (256, 144))
    rd = ramp.load()
    for j in range(144):
        for i in range(256):
            d = (i / 255.0) * 0.55 + (1.0 - j / 143.0) * 0.75
            t = (d - (0.62 - 0.34)) / 0.68
            rd[i, j] = round(255 * max(0.0, min(1.0, t * t * (3 - 2 * t))))
    sky = Image.composite(night, day, ramp.resize(size, Image.BICUBIC))

    haze = Image.new("RGBA", size, (0, 0, 0, 0))
    hd = ImageDraw.Draw(haze)
    for y, thick, alpha in [(0.66, 0.030, 46), (0.58, 0.022, 34), (0.47, 0.026, 28)]:
        hd.ellipse([-0.25 * w, (1 - y - thick) * h, 1.25 * w, (1 - y + thick) * h],
                   fill=HAZE + (alpha,))
    haze = haze.filter(ImageFilter.GaussianBlur(h * 0.018))
    return Image.alpha_composite(sky.convert("RGBA"), haze)


def recede(layer: Image.Image, amount: float) -> Image.Image:
    """Wash a layer toward the sky haze so distance reads as distance."""
    veil = Image.new("RGBA", layer.size, HAZE + (255,))
    faded = Image.blend(layer.convert("RGBA"), veil, amount)
    faded.putalpha(layer.split()[3])
    return faded


def build_far(size, rng) -> Image.Image:
    c = Canvas(size)
    ice_face, ice_cliff = plateau(ICE, rng, DEPTH_FAR)
    c.shape(ridge_outline(ridge_base(ICE, DEPTH_FAR), rng), ICE_CLIFF, ICE_RIM)
    c.shape(ridge_outline(CITADEL, rng), CITADEL_FACE, CITADEL_RIM)
    c.shape(ice_cliff, ICE_CLIFF)
    c.shape(ice_face, ICE_FACE, ICE_RIM)

    sand_face, sand_cliff = plateau(SAND, rng, DEPTH_FAR)
    c.shape(ridge_outline(ridge_base(SAND, DEPTH_FAR), rng), SAND_CLIFF, SAND_RIM)
    c.shape(sand_cliff, SAND_CLIFF)
    c.shape(sand_face, SAND_FACE, SAND_RIM)
    return recede(c.img, FAR_HAZE)


def build_mid(size, rng) -> Image.Image:
    c = Canvas(size)

    cloud = STORM_CLOUD_BAND
    puffs = Image.new("RGBA", size, (0, 0, 0, 0))
    pd = ImageDraw.Draw(puffs)
    for _ in range(14):
        x = STORM["centre"] + rng.uniform(-1.05, 0.95) * STORM["half"]
        y = cloud["y"] + rng.uniform(-0.6, 0.6) * cloud["thick"]
        r = rng.uniform(0.30, 0.72) * cloud["thick"]
        pd.ellipse([(x - r * 1.5) * c.w, (1 - y - r) * c.h,
                    (x + r * 1.5) * c.w, (1 - y + r) * c.h], fill=STORM_CLOUD + (255,))
    c.img = Image.alpha_composite(c.img, puffs.filter(ImageFilter.GaussianBlur(c.h * 0.004)))
    c.draw = ImageDraw.Draw(c.img)
    storm_face, storm_cliff = plateau(STORM, rng, DEPTH_MID)
    c.shape(ridge_outline(ridge_base(STORM, DEPTH_MID), rng), STORM_CLIFF, STORM_RIM)
    c.shape(storm_cliff, STORM_CLIFF)
    c.shape(storm_face, STORM_FACE, STORM_RIM)

    fire_face, fire_cliff = plateau(FIRE, rng, DEPTH_MID)
    c.shape(ridge_outline(ridge_base(FIRE, DEPTH_MID), rng), ROCK_CLIFF, ROCK_RIM)
    c.shape(fire_cliff, ROCK_CLIFF)
    c.shape(fire_face, ROCK_FACE, ROCK_RIM)
    for _ in range(26):                                  # short cracks in the crust
        x = FIRE["centre"] + rng.uniform(-0.85, 0.85) * FIRE["half"]
        y = FIRE["top"] - rng.uniform(0.004, 0.055)
        crack = [(x, y)]
        for _ in range(rng.randint(3, 6)):
            x += rng.uniform(-0.018, 0.018)
            y -= rng.uniform(0.002, 0.007)
            crack.append((x, y))
        c.draw.line(c.px(crack), fill=LAVA + (255,), width=max(1, round(c.h * 0.0018)))

    for i, fall in enumerate(FALLS):
        top, bottom = POOLS[i]["top"], POOLS[i + 1]["top"] - 0.012
        wide, thin = fall["half"], fall["half"] * 0.72
        ribbon = [(fall["x"] - wide, top), (fall["x"] + wide, top),
                  (fall["x"] + thin, bottom), (fall["x"] - thin, bottom)]
        c.shape(ribbon, WATER_DEEP)
        c.draw.line(c.px([(fall["x"] - wide * 0.45, top), (fall["x"] - thin * 0.45, bottom)]),
                    fill=WATER_SHALLOW + (255,), width=max(1, round(c.h * 0.0022)))
    for pool in POOLS:
        rim_cfg = dict(pool, half=pool["half"] * 1.06, top=pool["top"] + 0.004)
        rim_face, rim_cliff = plateau(rim_cfg, rng, DEPTH_MID)
        c.shape(rim_cliff, BASIN_CLIFF)
        c.shape(rim_face, BASIN_FACE)
        water_face, _ = plateau(pool, rng, DEPTH_MID * 0.88, level=True)
        c.shape(water_face, WATER_SHALLOW, WATER_RIM, rim_size=0.004)
    return recede(c.img, MID_HAZE)


def build_near(size, rng) -> Image.Image:
    c = Canvas(size)
    c.shape(canopy_outline(RIDGE["base"], RIDGE["pitch"], RIDGE["lo"], RIDGE["hi"], rng), RIDGE_FACE)
    c.shape(canopy_outline(JUNGLE["base"], JUNGLE["pitch"], JUNGLE["lo"], JUNGLE["hi"], rng),
            JUNGLE_FACE, JUNGLE_RIM)
    c.shape(canopy_outline(JUNGLE["base"] + 0.020, JUNGLE["pitch"] * 0.61,
                           JUNGLE["lo"] * 0.65, JUNGLE["hi"] * 0.65, rng), JUNGLE_FACE, JUNGLE_RIM)
    c.shape(canopy_outline(JUNGLE["base"] - 0.035, JUNGLE["pitch"] * 1.45,
                           JUNGLE["lo"] * 1.1, JUNGLE["hi"] * 1.15, rng), JUNGLE_FACE, JUNGLE_RIM)
    c.shape(path_outline(), PATH_FACE, PATH_RIM, rim_size=0.004)

    frame = Image.new("RGBA", size, CAVE_ROCK + (255,))
    hole = Image.new("L", size, 0)
    ImageDraw.Draw(hole).polygon(c.px(mouth_outline()), fill=255)
    frame.putalpha(hole.point(lambda v: 255 - v))
    c.img = Image.alpha_composite(c.img, frame)
    c.draw = ImageDraw.Draw(c.img)

    for shard in shard_outlines(rng):
        c.shape(shard, CRYSTAL_FACE, CRYSTAL_RIM, rim_size=0.004)
    return c.img


def build_masks(size, rng) -> Image.Image:
    """R = pool surface, G = waterfall, B = storm rock, A = open sky."""
    w, h = size
    red, green, blue, alpha = (Image.new("L", size, 0) for _ in range(4))

    rd = ImageDraw.Draw(red)
    for pool in POOLS:
        face, _ = plateau(pool, rng, DEPTH_MID * 0.88, level=True)
        rd.polygon([(x * w, (1 - y) * h) for x, y in face], fill=255)

    gd = ImageDraw.Draw(green)
    for i, fall in enumerate(FALLS):
        top, bottom = POOLS[i]["top"], POOLS[i + 1]["top"] - 0.005
        gd.rectangle([(fall["x"] - fall["half"]) * w, (1 - top) * h,
                      (fall["x"] + fall["half"]) * w, (1 - bottom) * h], fill=255)

    bd = ImageDraw.Draw(blue)
    face, cliff = plateau(STORM, rng, DEPTH_MID)
    for pts in (face, cliff, ridge_outline(ridge_base(STORM, DEPTH_MID), rng)):
        bd.polygon([(x * w, (1 - y) * h) for x, y in pts], fill=255)

    # Sky is whatever no layer covers, so it is derived rather than drawn.
    return Image.merge("RGBA", (red, green, blue, alpha))


def sky_mask(size, far, mid, near) -> Image.Image:
    covered = Image.new("L", size, 0)
    for layer in (far, mid, near):
        covered = ImageChops.lighter(covered, layer.split()[3])
    return covered.point(lambda v: 255 - v).filter(ImageFilter.GaussianBlur(1.0))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--size", nargs=2, type=int, default=[1920, 1080])
    ap.add_argument("--seed", type=int, default=SEED)
    ap.add_argument("--out", type=Path, default=OUT_DIR)
    args = ap.parse_args()

    size = (args.size[0], args.size[1])
    args.out.mkdir(parents=True, exist_ok=True)

    far = build_far(size, random.Random(args.seed))
    mid = build_mid(size, random.Random(args.seed + 1))
    near = build_near(size, random.Random(args.seed + 2))
    masks = build_masks(size, random.Random(args.seed + 3))
    r, g, b, _ = masks.split()
    masks = Image.merge("RGBA", (r, g, b, sky_mask(size, far, mid, near)))

    for name, img in [("sky", build_sky(size).convert("RGB")), ("far", far),
                      ("mid", mid), ("near", near), ("masks", masks)]:
        path = args.out / f"{name}.png"
        img.save(path)
        print(f"wrote {path} {img.size} {img.mode}")


if __name__ == "__main__":
    main()
