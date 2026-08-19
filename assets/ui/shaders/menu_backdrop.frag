// Menu backdrop: the painted mainmenu_background.png with its living regions
// animated in place â€” water, dust, frost, wind and clouds are found in the
// painting by colour keys gated to hand-placed boxes; the stars are a layer baked
// by tools/extract_menu_layers.py, and the lightning is drawn from scratch, so the
// base painting ships without either.
// Blend: none â€” this is an opaque background pass.

#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;   // mainmenu_background.png, bound by SpriteBatch on unit 0
uniform sampler2D u_stars;     // ui/menu_stars.png â€” the sky's stars on transparent
uniform float u_time;          // seconds, free-running
uniform float u_aspect;        // width / height, so ripples aren't stretched
uniform float u_intensity;     // master multiplier on every mover, 0 = still painting

// Image space `uv` is 0..1 with y DOWN, exactly like the png: (0,0) is top-left.

// ---- tuning: clouds --------------------------------------------------------
// The painted cloud banks stay put but live: they boil slowly inside their own
// pixels, light rolls across them, faint cirrus drifts through the open sky and
// the low sun throws rays across the left. Keys: clouds are pale and low in
// saturation with blue >= green (keeps the tan path and sand out); storm banks
// are dark purple; open sky is blue.
const vec4  SUNSET_BOX     = vec4(0.00, 0.00, 0.44, 0.60); // the pink/lavender banks behind the mesas
const vec4  MIST_BOX       = vec4(0.00, 0.50, 0.50, 1.00); // the lavender mist under the shelves
const vec4  STORM_CLOUDS   = vec4(0.68, 0.00, 1.00, 0.32); // the dark purple bank
const float CLOUD_SAT_MAX  = 0.26;   // saturation above this = not cloud
const float CLOUD_LUM_MIN  = 0.66;   // luminance below this = not (pale) cloud â€” the grey cliff faces sit just under it
const float BOIL_SCALE     = 3.5;    // billow cells across the frame; lower = broader swells
const float BOIL_SPEED     = 0.035;  // how fast the billows drift
const float BOIL_AMP       = 0.006;  // how far the cloud paint is kneaded, in uv
const float LIGHT_SCALE    = 1.6;    // size of the light patches rolling over the banks
const float LIGHT_SPEED    = 0.02;   // how fast they roll (frame widths per second)
const float LIGHT_GAIN     = 0.16;   // brightening at a patch's centre
const vec3  SUN_WARM       = vec3(1.00, 0.80, 0.55); // tint the light adds on the sunset banks
const float STORM_LIGHT    = 0.10;   // same idea on the storm bank, cooler and dimmer
const vec4  CIRRUS_BOX     = vec4(0.12, -0.05, 0.75, 0.24); // open sky the wisps may cross
const float CIRRUS_FEATHER = 0.10;   // how softly the wisps fade at the box's edges
const float CIRRUS_SCALE   = 9.0;    // wisp grain
const float CIRRUS_STRETCH = 7.0;    // how much longer than tall a wisp is
const float CIRRUS_CURL    = 0.6;    // how much the wisps bend (domain warp), 0 = straight
const float CIRRUS_PATCH   = 1.4;    // size of the clumps the wisps gather in; lower = broader
const float CIRRUS_SPEED   = 0.012;  // drift, frame widths per second
const float CIRRUS_LEVEL   = 0.22;   // peak opacity
const vec3  CIRRUS_TINT    = vec3(0.90, 0.93, 1.00);
const vec2  SUN_POS        = vec2(0.16, 0.27);   // the low sun's disc, image space
const float RAY_COUNT      = 22.0;   // rays around the sun; higher = finer
const float RAY_SPEED      = 0.08;   // slow wheel of the rays, radians per second
const float RAY_REACH      = 0.55;   // how far the longest rays carry, in uv
const float RAY_REACH_VARY = 0.55;   // 0 = every ray the same length, 1 = some barely leave the sun
const float RAY_BREATHE    = 0.35;   // how fast each ray fades in and out on its own
const float RAY_GAIN       = 0.11;   // brightness at the sun
const vec3  RAY_TINT       = vec3(1.00, 0.85, 0.60);

// ---- tuning: water ---------------------------------------------------------
// The pools are found by their teal, gated to the terrace region so ice and sky
// don't count as water. Falls are the same teal, so they are placed by hand.
const vec4  WATER_BOX      = vec4(0.24, 0.44, 0.46, 0.86); // x0,y0,x1,y1 of the terraces
const float WATER_KEY_LO   = 0.10;   // min(g,b)-r below this = not water
const float WATER_KEY_HI   = 0.30;   // ...above this = fully water
const float RIPPLE_SCALE   = 55.0;   // ripple cells across the frame; higher = finer chop
const float RIPPLE_SPEED   = 0.50;   // how fast the surface crawls
const float RIPPLE_REFRACT = 0.0022; // how far the surface bends what's under it, in uv units
const float RIPPLE_SHADE   = 0.22;   // light/dark from the wave slope, 0 = flat
const float GLINT_SCALE    = 300.0;  // sparkle grain; higher = smaller glints
const float GLINT_SPEED    = 0.85;
const float GLINT_CUT      = 0.52;   // crest threshold 0..1; raise for fewer glints
const float GLINT_GAIN     = 0.80;
const vec3  GLINT_TINT     = vec3(0.92, 1.00, 1.00);
const float FALL_SOFT      = 0.006;  // edge feather of each fall box
const float FALL_RIBS      = 150.0;  // foam ribs down a fall; higher = finer
const float FALL_SPEED     = 1.10;   // how fast the water drops
const float FALL_FOAM      = 0.45;   // brightness of the ribs
const vec3  FOAM_TINT      = vec3(0.90, 0.99, 1.00);
const vec4  FALL_A = vec4(0.335, 0.515, 0.356, 0.580); // top centre
const vec4  FALL_B = vec4(0.270, 0.615, 0.290, 0.675); // left middle
const vec4  FALL_C = vec4(0.308, 0.715, 0.334, 0.845); // long bottom fall
const vec4  FALL_D = vec4(0.400, 0.525, 0.432, 0.590); // right cascade

// ---- tuning: lightning -----------------------------------------------------
// The bolt is drawn from scratch: a jagged trunk from the cloud base to the storm
// rock, re-rolled every strike, with thinner forks off it. A leader runs it down,
// then it blows white with a wide halo, splashes where it hits and flashes the storm.
const vec4  STORM_BOX     = vec4(0.82, 0.17, 0.95, 0.44); // x0,y0,x1,y1 the storm corner (flash / pre-flash anchor)
const float BOLT_X        = 0.875; // where the trunk leaves the cloud, on average
const float BOLT_TOP      = 0.19;  // cloud base
const float BOLT_BOTTOM   = 0.425; // where it hits the rock
const float BOLT_WANDER   = 0.035; // how far left/right of BOLT_X a strike may start
const float BOLT_LEAN     = 0.04;  // max sideways drift from cloud to ground
const float BOLT_KINKS    = 8.0;   // major zigzags down the trunk
const float BOLT_KINK_AMP = 0.030; // how far the major zigzags swing, in uv
const float BOLT_JITTER   = 0.009; // fine jaggedness on top
const float BOLT_WIDTH    = 0.0030;// core half-width, aspect-corrected uv
const float BOLT_HALO     = 0.030; // reach of the glow hugging the core
const float HALO_TAIL     = 3.5;   // the faint outer glow reaches this many times further
const float HALO_GAIN     = 1.4;
const float BRANCH_LEN    = 0.45;  // fraction of the trunk's drop a fork covers
const float BRANCH_LEAN   = 0.10;  // sideways reach of a fork
const float BRANCH_WIDTH  = 0.55;  // fork core width vs the trunk's
const float SPLASH_REACH  = 0.045; // glow where the bolt meets the rock
const float SPLASH_GAIN   = 0.9;
const vec2  BOLT_ORIGIN   = vec2(0.87, 0.30);  // centre of the flash the strike throws
const float STRIKE_PERIOD = 3.2;   // seconds per slot in which a strike may land
const float STRIKE_RARITY = 0.35;  // fraction of slots left empty; higher = rarer
const float STRIKE_LEAD   = 0.10;  // seconds for the bolt to run cloud -> ground
const float STRIKE_DECAY  = 4.5;   // how fast a strike dies, per second
const float CRACKLE_HZ    = 30.0;  // stutter rate while a strike is alive
const float CRACKLE_FLOOR = 0.35;  // how dark the stutter's off-beat goes
const float FLASH_REACH   = 0.22;  // how far the strike lights clouds and rock
const float FLASH_GAIN    = 0.65;
const vec3  BOLT_CORE     = vec3(1.00, 1.00, 1.00);
const vec3  BOLT_GLOW     = vec3(0.84, 0.78, 1.00);
// Sheet lightning: soft glows inside the storm bank, on their own beat, plus a
// pre-flash in the cloud a moment before the bolt drops.
const float SHEET_PERIOD  = 2.1;   // seconds per slot in which a sheet flash may happen
const float SHEET_RARITY  = 0.55;  // fraction of slots left dark
const float SHEET_DECAY   = 6.0;   // how fast a sheet flash dies, per second
const float SHEET_REACH   = 0.09;  // uv radius of a sheet flash
const float SHEET_GAIN    = 0.55;
const float PRE_FLASH     = 0.30;  // seconds before the bolt the cloud starts to glow
const float PRE_GAIN      = 0.35;
const vec3  SHEET_TINT    = vec3(0.80, 0.72, 1.00);
// Crackle: small purple forks snapping through the bank on their own slots.
const float CRACKLE_PERIOD = 1.7;   // seconds per slot in which a fork may snap
const float CRACKLE_RARITY = 0.35; // fraction of slots left dark
const float CRACKLE_LEN    = 0.07;  // length of a fork, in uv
const float CRACKLE_KINK   = 0.012; // how far it zigzags off its line
const float CRACKLE_WIDTH  = 0.0016;// half-thickness of the core
const float CRACKLE_HALO   = 0.014; // reach of its glow
const float CRACKLE_DECAY  = 5.5;   // how fast it dies, per second
const vec3  CRACKLE_CORE   = vec3(0.95, 0.90, 1.00);
const vec3  CRACKLE_GLOW   = vec3(0.70, 0.45, 1.00);

// ---- tuning: stars ---------------------------------------------------------
// The star layer (u_stars), each star breathing on its own phase.
const float TWINKLE_SPEED  = 0.4;    // breaths per second, roughly
const float TWINKLE_DIM    = 0.30;   // how faint a star gets at the bottom of its breath
const float TWINKLE_FLARE  = 0.30;   // extra glow at the top of it
const float TWINKLE_GRAIN  = 30.0;   // how close two stars must be to share a phase; higher = more independent
const vec3  STAR_TINT      = vec3(0.92, 0.95, 1.00);

// ---- tuning: tower ---------------------------------------------------------
// The citadel's rings and tip star hold a steady glow: their own pixels are lit
// in place (no spill, no motion), breathing very slightly.
const vec4  TOWER_BOX      = vec4(0.38, 0.00, 0.66, 0.15); // x0,y0,x1,y1 around the rings (the spire body stays as painted)
const float RING_KEY_LO    = 0.10;   // b - g below this = sky or white spire, not a lavender ring
const float RING_KEY_HI    = 0.22;
const float RING_GLOW      = 0.55;   // how much light the rings themselves gain
const float TOWER_BREATHE  = 0.12;   // slow swell of the glow, 0 = steady
const vec3  TOWER_TINT     = vec3(0.78, 0.74, 1.00);
const vec2  TIP_POS        = vec2(0.517, 0.064); // the star on the spire's tip
const float TIP_REACH      = 0.012;  // radius of the tip's soft light, in uv
const float TIP_GAIN       = 0.45;
const vec3  TIP_TINT       = vec3(0.95, 0.92, 1.00);

// ---- tuning: wind ----------------------------------------------------------
// Wind is shown, not simulated: thin streaks sweep left to right across the
// jungle band, and torn leaves tumble along the same lanes.
const vec4  WIND_BAND      = vec4(0.54, 0.34, 0.99, 0.80); // x0,y0,x1,y1 the wind blows through
const vec2  WIND_START     = vec2(0.555, 0.60);  // x where lanes begin at the top / bottom of the band: the path's right edge
const float WIND_DESCENT   = 0.10;   // how much a lane drops across the band (uv per uv)
const float STREAK_COUNT   = 6.0;
const float STREAK_LEN     = 0.14;   // length of a streak, in aspect-corrected units
const float STREAK_SPEED   = 0.42;   // units per second
const float STREAK_GAP     = 2.5;    // seconds a lane rests between streaks
const float STREAK_WAVE    = 0.030;  // amplitude of the S-curve
const float STREAK_WAVES   = 4.5;    // bends per unit
const float STREAK_WIDTH   = 0.0022; // half-thickness at the belly
const float STREAK_ALPHA   = 0.65;
const vec3  STREAK_TINT    = vec3(0.95, 0.98, 1.00);
const float LEAF_COUNT     = 22.0;
const float LEAF_SPEED_MIN = 0.16;
const float LEAF_SPEED_MAX = 0.32;
const float LEAF_SIZE      = 0.0050; // long radius, in aspect-corrected units
const float LEAF_BOB       = 0.018;  // vertical wobble as it flies
const float LEAF_TUMBLE    = 5.0;    // spins per second
const vec3  LEAF_TINT_A    = vec3(0.52, 0.60, 0.22);
const vec3  LEAF_TINT_B    = vec3(0.30, 0.42, 0.16);
// Depth: lane 0 is the top of the band (far), lane 1 the bottom (near).
const float WIND_FAR_SCALE = 0.45;   // size and speed of the farthest lane vs the nearest
const float WIND_FAR_ALPHA = 0.45;   // brightness of the farthest lane vs the nearest
const float WIND_HIDE_FAR  = 0.95;   // how fully foliage hides a far streak/leaf
const float WIND_HIDE_NEAR = 0.30;   // ...and a near one, which is mostly in front of it
const float CANOPY_KEY_LO  = -0.01;  // g - max(r,b) below this = not foliage (it's olive, so ~0)
const float CANOPY_KEY_HI  = 0.03;
const vec2  CAVE_WALL_EDGE = vec2(1.06, -0.28); // the wall's left silhouette: x = a + b*y (image space)
const float CAVE_WALL_X    = 0.80;   // right of here, dark rock is also cave mouth (the jagged bits)
const float CAVE_WALL_LUM  = 0.20;   // luminance below this = wall

// ---- tuning: fire ----------------------------------------------------------
// The lava is found by its orange inside the fire box. It flows (hot spots
// travel downhill and the paint is dragged a touch with them), throws warm light
// on the rock around it, and the air above it shimmers.
const vec4  FIRE_BOX       = vec4(0.00, 0.52, 0.27, 0.96); // x0,y0,x1,y1 of the volcanic shelf
const float LAVA_KEY_LO    = 0.25;   // r - g below this = rock, not lava
const float LAVA_KEY_HI    = 0.45;
const vec2  LAVA_FLOW      = vec2(0.55, 0.85);  // downhill direction in image space (y down)
const float FLOW_SCALE     = 45.0;   // hot-spot grain along the channels; higher = finer
const float FLOW_SPEED     = 0.012;  // uv per second the hot spots travel
const float FLOW_CONTRAST  = 0.45;   // how much brighter / darker the travelling spots are
const float FLOW_SHIFT     = 0.0018; // uv the painted lava is dragged along the flow
const vec3  LAVA_HOT       = vec3(1.00, 0.88, 0.50);  // colour of the hottest crests
const float GLOW_BLUR      = 4.5;    // mip bias of the light the lava throws; higher = wider, softer
const float GLOW_GAIN      = 0.75;   // strength of that light on the rock
const float GLOW_PULSE     = 0.30;   // slow breathing of the glow, 0 = steady
const vec3  GLOW_TINT      = vec3(1.00, 0.42, 0.10);
const float HEAT_RISE      = 0.06;   // how far above the lava the shimmer reaches, in uv
const float HEAT_SCALE     = 28.0;   // shimmer grain
const float HEAT_SPEED     = 1.6;    // how fast the shimmer rises, cells per second
const float HEAT_AMP       = 0.0035; // uv distortion at full strength

// ---- tuning: dust ----------------------------------------------------------
// Sand lifts off the plateau: a warm haze streams off its right edge and loose
// grains rise from the surface and blow away in the same direction.
const vec4  PLATEAU_BOX    = vec4(0.13, 0.36, 0.36, 0.46); // x0,y0,x1,y1 of the plateau top (the pools start just below)
const float HAZE_REACH     = 0.05;   // how far past the plateau's right edge the haze streams into the sky
const float HAZE_SCALE     = 32.0;   // wisp grain; higher = finer
const float HAZE_STRETCH   = 3.5;    // how much longer wisps are than they are tall
const float HAZE_SPEED     = 0.06;   // drift, frame widths per second
const float HAZE_LEVEL     = 0.60;   // peak opacity of the haze
const vec3  HAZE_TINT      = vec3(1.00, 0.90, 0.70);
const float GRAIN_COUNT    = 90.0;
const float GRAIN_LIFE     = 4.0;    // seconds a grain is airborne
const float GRAIN_DRIFT    = 0.045;  // uv it travels right over its life
const float GRAIN_LIFT     = 0.020;  // uv it rises over its life
const float GRAIN_SIZE     = 0.0015; // radius, in aspect-corrected units
const float GRAIN_ALPHA    = 0.95;
const vec3  GRAIN_TINT     = vec3(1.00, 0.92, 0.72);
const float DUST_FAR_SCALE = 0.45;   // grain size at the far (top) edge of the plateau vs the near (bottom)
const float DUST_FAR_ALPHA = 0.40;   // haze and grain opacity at the far edge vs the near
const float SAND_KEY_LO    = 0.07;   // r - b below this = not sand
const float SAND_KEY_HI    = 0.18;

// ---- tuning: frost ---------------------------------------------------------
// Cold vapour clings to the ice shelf and sinks off its edges; ice motes glint
// as they drift with it. Slight, and far away, so everything here is small.
const vec4  ICE_BOX        = vec4(0.55, 0.22, 0.75, 0.40); // x0,y0,x1,y1 of the shelf, spires included
const float ICE_KEY_LO     = 0.62;   // luminance below this = not ice
const float ICE_KEY_HI     = 0.80;
const float FROST_SPILL    = 0.05;   // how far the vapour sinks below / past the shelf
const float FROST_SCALE    = 40.0;   // wisp grain; higher = finer
const float FROST_STRETCH  = 2.5;    // wisps are wider than tall
const float FROST_SPEED    = 0.025;  // drift, frame widths per second (down-right)
const float FROST_LEVEL    = 0.38;   // peak opacity of the vapour
const vec3  FROST_TINT     = vec3(0.86, 0.94, 1.00);
const float MOTE_COUNT     = 50.0;
const float MOTE_LIFE      = 6.0;    // seconds a mote drifts
const float MOTE_DRIFT     = 0.03;   // uv it travels right over its life
const float MOTE_SINK      = 0.03;   // uv it sinks over its life
const float MOTE_SIZE      = 0.0012; // radius, in aspect-corrected units
const float MOTE_TWINKLE   = 6.0;    // glints per second
const vec3  MOTE_TINT      = vec3(0.95, 0.99, 1.00);
// The heavy fall: dense fog pouring over the shelf's lower lip and thinning below.
// The lip is read from the painting â€” fog sits wherever ice is a little way above.
const vec4  LEDGE_BOX      = vec4(0.605, 0.34, 0.745, 0.48); // x0,y0,x1,y1 the fall may occupy
const float FOG_DROP       = 0.045;  // how far below an ice edge the fog reaches, in uv
const float FOG_SCALE      = 48.0;   // billow size; higher = smaller billows
const float FOG_SPEED      = 0.035;  // how fast the fog pours down, frame heights per second
const float FOG_LEVEL      = 0.75;   // opacity right under the lip
const float FOG_ON_ICE     = 0.25;   // how much shows over the ice face itself vs. the open air below it
const float FOG_BLUR       = 3.5;    // mip bias for the edge test; higher = softer lip
const vec3  FOG_TINT       = vec3(0.90, 0.96, 1.00);

// ---- helpers ---------------------------------------------------------------

float hash11(float n) {
    return fract(sin(n * 127.1) * 43758.5453);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// Smooth value noise, 0..1.
float valueNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i),               hash21(i + vec2(1, 0)), f.x),
               mix(hash21(i + vec2(0, 1)),  hash21(i + vec2(1, 1)), f.x), f.y);
}

// 1 inside the box, feathering to 0 over `soft` outside it.
float softBox(vec2 uv, vec4 box, float soft) {
    vec2 d = max(box.xy - uv, uv - box.zw);
    return 1.0 - smoothstep(0.0, soft, max(d.x, d.y));
}

// ---- clouds ----------------------------------------------------------------

// Pale, unsaturated, blue-not-below-green: the sunset banks and the mist.
float cloudKey(vec3 c, vec2 uv) {
    float lum = (c.r + c.g + c.b) / 3.0;
    float sat = max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
    float pale = smoothstep(CLOUD_LUM_MIN, CLOUD_LUM_MIN + 0.12, lum) * smoothstep(CLOUD_SAT_MAX + 0.08, CLOUD_SAT_MAX, sat)
               * smoothstep(-0.04, 0.02, c.b - c.g);
    return pale * max(softBox(uv, SUNSET_BOX, 0.02), softBox(uv, MIST_BOX, 0.02));
}

// Dark purple: the storm bank (vines and rock are greener / greyer).
float stormKey(vec3 c, vec2 uv) {
    float lum = (c.r + c.g + c.b) / 3.0;
    return smoothstep(0.08, 0.16, c.b - c.g) * smoothstep(0.62, 0.45, lum) * smoothstep(0.18, 0.30, lum)
         * softBox(uv, STORM_CLOUDS, 0.02);
}

// Open blue sky, above the ice spires.
float skyKey(vec3 c, vec2 uv) {
    float lum = (c.r + c.g + c.b) / 3.0;
    return smoothstep(0.12, 0.25, c.b - c.r) * smoothstep(0.85, 0.70, lum) * softBox(uv, CIRRUS_BOX, CIRRUS_FEATHER);
}

// Broad soft light patches drifting right, 0..1.
float rollingLight(vec2 q, float t) {
    return valueNoise(q * LIGHT_SCALE - vec2(t * LIGHT_SPEED, t * LIGHT_SPEED * 0.2));
}

// Long thin streaks drifting right through the sky, bending a little and
// gathering in clumps rather than filling a band, 0..1.
float cirrus(vec2 q, float t) {
    float curl  = (valueNoise(q * 1.3 + vec2(t * 0.01, 0.0)) - 0.5) * CIRRUS_CURL;
    vec2  p     = vec2(q.x / CIRRUS_STRETCH - t * CIRRUS_SPEED, q.y + curl) * CIRRUS_SCALE;
    float n     = valueNoise(p) * 0.6 + valueNoise(p * 2.0 + vec2(0.0, t * 0.02)) * 0.4;
    float patch = smoothstep(0.35, 0.7, valueNoise(q * CIRRUS_PATCH + vec2(-t * CIRRUS_SPEED * 0.5, 4.0)));
    return smoothstep(0.55, 0.78, n) * patch;
}

// Shafts of light wheeling slowly around the sun: each ray its own length,
// each breathing in and out on its own, all fading with distance.
float godRays(vec2 uv, float t) {
    vec2  d     = (uv - SUN_POS) * vec2(u_aspect, 1.0);
    float ang   = atan(d.y, d.x) * RAY_COUNT / 6.28318;
    float shaft = valueNoise(vec2(ang * 4.0 + t * RAY_SPEED, 0.5))
                * valueNoise(vec2(ang * 9.0 - t * RAY_SPEED * 0.6, 3.5));
    float reach = RAY_REACH * (1.0 - RAY_REACH_VARY * valueNoise(vec2(ang * 2.5 + t * RAY_SPEED * 0.3, 8.0)));
    float alive = 0.55 + 0.45 * sin(t * RAY_BREATHE * 6.28318 * 0.25 + valueNoise(vec2(ang * 3.0, 12.0)) * 12.0);
    float carry = 1.0 - smoothstep(0.0, reach, length(d));
    return smoothstep(0.15, 0.55, shaft) * carry * carry * alive;
}

vec3 clouds(vec3 col, vec3 still, vec2 uv, float t) {
    vec2  q     = vec2(uv.x * u_aspect, uv.y);
    float cloud = cloudKey(still, uv) * u_intensity;
    float storm = stormKey(still, uv) * u_intensity;

    // Boil: knead the cloud paint with a slow swell, only where both ends are cloud.
    vec2  swell = vec2(valueNoise(q * BOIL_SCALE + vec2(t * BOIL_SPEED, 0.0)) - 0.5,
                       valueNoise(q * BOIL_SCALE + vec2(11.0, t * BOIL_SPEED * 0.7)) - 0.5) * BOIL_AMP;
    float bank  = max(cloud, storm);
    vec3  knead = texture2D(u_texture, uv + swell * bank).rgb;
    float there = max(cloudKey(knead, uv), stormKey(knead, uv));
    col = mix(col, knead, bank * there);

    // Light rolling across the banks: warm on the sunset side, cool on the storm.
    float light = rollingLight(q, t) - 0.5;
    col += SUN_WARM * light * LIGHT_GAIN * cloud;
    col += vec3(0.75, 0.70, 1.0) * light * STORM_LIGHT * storm;

    // Cirrus through the open sky, and the sun's rays across the left.
    float sky = skyKey(still, uv) * (1.0 - storm) * u_intensity;   // never over the storm bank
    col = mix(col, CIRRUS_TINT, cirrus(q, t) * CIRRUS_LEVEL * sky);
    float lum  = (still.r + still.g + still.b) / 3.0;
    float sat  = max(still.r, max(still.g, still.b)) - min(still.r, min(still.g, still.b));
    float pale = smoothstep(0.50, 0.65, lum) * smoothstep(0.45, 0.30, sat) * softBox(uv, SUNSET_BOX, 0.05);   // hazy sky, not wall or sand
    float rays = godRays(uv, t) * max(cloud, pale) * u_intensity;
    col += RAY_TINT * rays * RAY_GAIN;
    return col;
}

// ---- water -----------------------------------------------------------------

// How much of this pixel is teal water: the pool key, gated to the terraces.
float wetness(vec3 c, vec2 uv) {
    float teal = smoothstep(WATER_KEY_LO, WATER_KEY_HI, min(c.g, c.b) - c.r)
               * smoothstep(0.25, 0.50, max(c.g, c.b));
    return teal * softBox(uv, WATER_BOX, 0.0);
}

// Two crossing sheets of chop, drifting against each other, as a height field.
float surfaceHeight(vec2 q, float t) {
    float a = valueNoise(q       + vec2( t * RIPPLE_SPEED,       t * RIPPLE_SPEED * 0.6));
    float b = valueNoise(q * 2.3 + vec2(-t * RIPPLE_SPEED * 1.4, t * RIPPLE_SPEED * 0.3));
    return a * 0.65 + b * 0.35;
}

// Slope of the surface, used both to bend the sample and to light it.
vec2 surfaceSlope(vec2 q, float t) {
    float e  = 0.15;
    float h  = surfaceHeight(q, t);
    float hx = surfaceHeight(q + vec2(e, 0.0), t);
    float hy = surfaceHeight(q + vec2(0.0, e), t);
    return vec2(hx - h, hy - h) / e;
}

// Sparse moving sparkle where the light catches a crest.
float glints(vec2 q, float t) {
    float k = GLINT_SCALE / RIPPLE_SCALE;
    float n = valueNoise(q * k       + vec2(t * GLINT_SPEED, -t * GLINT_SPEED * 0.7))
            * valueNoise(q * k * 1.7 - vec2(t * GLINT_SPEED * 0.5, 0.0));
    return smoothstep(GLINT_CUT, GLINT_CUT + 0.08, n) * GLINT_GAIN;
}

// Where the hand-placed falls are, 0..1.
float fallMask(vec2 uv) {
    return max(max(softBox(uv, FALL_A, FALL_SOFT), softBox(uv, FALL_B, FALL_SOFT)),
               max(softBox(uv, FALL_C, FALL_SOFT), softBox(uv, FALL_D, FALL_SOFT)));
}

// Foam ribs sliding down a fall, broken up sideways so they don't read as stripes.
float fallFoam(vec2 uv, float t) {
    float wobble = valueNoise(vec2(uv.x * 400.0, uv.y * 30.0 - t * FALL_SPEED * 4.0)) - 0.5;
    float ribs   = 0.5 + 0.5 * sin((uv.y * FALL_RIBS - t * FALL_SPEED + wobble * 0.6) * 6.28318);
    return ribs * ribs * FALL_FOAM;
}

// ---- fire ------------------------------------------------------------------

// How much of this pixel is lava: hot orange inside the fire box.
float lavaKey(vec3 c, vec2 uv) {
    return smoothstep(LAVA_KEY_LO, LAVA_KEY_HI, c.r - c.g) * smoothstep(0.45, 0.65, c.r) * softBox(uv, FIRE_BOX, 0.0);
}

// Lava presence around a point, read through the painting's mipmaps: soft.
float lavaNearby(vec2 uv, float blur) {
    return lavaKey(texture2D(u_texture, uv, blur).rgb, uv);
}

// Hot spots travelling downhill along the channels, 0..1.
float lavaFlow(vec2 q, float t) {
    vec2  dir = normalize(vec2(LAVA_FLOW.x * u_aspect, LAVA_FLOW.y));
    vec2  p   = (q - dir * t * FLOW_SPEED) * FLOW_SCALE;
    return valueNoise(p) * 0.6 + valueNoise(p * 2.3 - dir * t * FLOW_SPEED * FLOW_SCALE * 0.5) * 0.4;
}

// Slow flicker shared by all the glow, like a furnace breathing.
float glowPulse(float t) {
    return 1.0 + GLOW_PULSE * (valueNoise(vec2(t * 0.8, 3.7)) - 0.5) * 2.0;
}

vec3 fire(vec3 col, vec3 still, vec2 uv, float t) {
    vec2  q    = vec2(uv.x * u_aspect, uv.y);
    float lava = lavaKey(still, uv) * u_intensity;

    // Flow: drag the paint along the channel and ride hot spots over it.
    vec2  dir     = normalize(LAVA_FLOW);
    float flow    = lavaFlow(q, t);
    float hot     = (flow - 0.5) * 2.0;
    vec3  dragged = texture2D(u_texture, uv + dir * FLOW_SHIFT * hot).rgb;
    vec3  glowing = dragged * (1.0 + hot * FLOW_CONTRAST) + LAVA_HOT * smoothstep(0.62, 0.85, flow) * 0.6;
    col = mix(col, glowing, lava);

    // Glow: the lava's own light spilling onto the rock around it.
    float near = lavaNearby(uv, GLOW_BLUR) * u_intensity;
    col += GLOW_TINT * near * GLOW_GAIN * glowPulse(t) * (1.0 - lava * 0.6);

    // Heat: rising shimmer over and above the lava, bending what's behind it.
    float heat = max(near, lavaNearby(uv + vec2(0.0, HEAT_RISE), GLOW_BLUR + 1.0) * u_intensity)
               * (1.0 - lava * 0.5);
    vec2  hq   = q * HEAT_SCALE + vec2(0.0, t * HEAT_SPEED);
    vec2  bend = vec2(valueNoise(hq) - 0.5, valueNoise(hq + 7.3) - 0.5) * HEAT_AMP * heat;
    vec3  seen = texture2D(u_texture, uv + bend).rgb;
    col = mix(col, seen + (col - still), heat * 0.85);   // keep the flow/glow we just added, bend the painting under it
    return col;
}

// ---- dust ------------------------------------------------------------------

// How much of this pixel is the plateau's sand: warm, red over blue.
float sandKey(vec3 c) {
    return smoothstep(SAND_KEY_LO, SAND_KEY_HI, c.r - c.b) * smoothstep(-0.02, 0.04, c.r - c.g);
}

// 0 at the far (top) edge of the plateau, 1 at the near (bottom) edge.
float dustDepth(float y) {
    return clamp((y - PLATEAU_BOX.y) / (PLATEAU_BOX.w - PLATEAU_BOX.y), 0.0, 1.0);
}

// Where the haze may hang: on the sand itself, and a short stream into the open
// sky past the plateau's right edge â€” never over the green cliffs beyond.
float hazeRegion(vec3 c, vec2 uv) {
    float onSand = sandKey(c) * softBox(uv, PLATEAU_BOX, 0.02);   // the flat top only, not the peak faces
    vec4  tailBox = vec4(PLATEAU_BOX.z - 0.03, PLATEAU_BOX.y, PLATEAU_BOX.z + HAZE_REACH, PLATEAU_BOX.w - 0.02);
    float notGreen = 1.0 - smoothstep(-0.02, 0.03, c.g - max(c.r, c.b));
    float tail = softBox(uv, tailBox, 0.03) * (1.0 - smoothstep(PLATEAU_BOX.z, tailBox.z, uv.x)) * notGreen;
    return max(onSand, tail);
}

// Long low wisps of sand drifting right.
float haze(vec2 uv, float t) {
    vec2  q  = vec2(uv.x * u_aspect / HAZE_STRETCH - t * HAZE_SPEED, uv.y + t * HAZE_SPEED * 0.3) * HAZE_SCALE;   // drifts right and lifts
    float n  = valueNoise(q) * 0.6 + valueNoise(q * 2.1 + vec2(t * 0.3, 0.0)) * 0.4;
    return smoothstep(0.32, 0.70, n);
}

// One grain: rises off the plateau, blows right, fades in and out over its life.
float grain(vec2 q, float i, float t) {
    float r0 = hash11(i * 2.9 + 0.5), r1 = hash11(i * 4.7 + 1.3);
    float r2 = hash11(i * 6.1 + 3.7), r3 = hash11(i * 7.3 + 5.9);
    float life  = mod(t / GRAIN_LIFE + r2, 1.0);                       // 0..1 over its flight
    vec2  start = vec2(mix(PLATEAU_BOX.x, PLATEAU_BOX.z, r0), mix(PLATEAU_BOX.y, PLATEAU_BOX.w, r1));
    float near  = dustDepth(start.y);
    float scale = mix(DUST_FAR_SCALE, 1.0, near);
    vec2  pos   = start + vec2(GRAIN_DRIFT * life, -GRAIN_LIFT * life + 0.006 * sin(t * 3.0 + r3 * 6.28318)) * scale;
    vec2  p     = q - vec2(pos.x * u_aspect, pos.y);
    float dot   = smoothstep(GRAIN_SIZE * scale, GRAIN_SIZE * scale * 0.3, length(p));
    float alive = smoothstep(0.0, 0.15, life) * smoothstep(1.0, 0.6, life)
                * (1.0 - smoothstep(PLATEAU_BOX.z + 0.02, PLATEAU_BOX.z + 0.06, pos.x));   // gone before the pools
    return dot * alive * mix(DUST_FAR_ALPHA, 1.0, near);
}

vec3 dust(vec3 col, vec3 still, vec2 uv, float t) {
    vec2  q = vec2(uv.x * u_aspect, uv.y);
    float wisps  = haze(uv, t) * hazeRegion(still, uv) * HAZE_LEVEL * mix(DUST_FAR_ALPHA, 1.0, dustDepth(uv.y));
    float grains = 0.0;
    for (float i = 0.0; i < GRAIN_COUNT; i += 1.0) grains += grain(q, i, t);
    col = mix(col, HAZE_TINT,  clamp(wisps, 0.0, 1.0) * u_intensity);
    col = mix(col, GRAIN_TINT, clamp(grains * GRAIN_ALPHA, 0.0, 1.0) * u_intensity);
    return col;
}

// ---- frost -----------------------------------------------------------------

// How much of this pixel is ice: pale and cool.
float iceKey(vec3 c) {
    float lum = (c.r + c.g + c.b) / 3.0;
    return smoothstep(ICE_KEY_LO, ICE_KEY_HI, lum) * smoothstep(-0.04, 0.02, c.b - c.r);
}

// Where vapour may hang: on the ice, and spilling a little below and past it.
float frostRegion(vec3 c, vec2 uv) {
    float onIce = iceKey(c) * softBox(uv, ICE_BOX, 0.02);
    vec4  spill = vec4(ICE_BOX.x + 0.04, ICE_BOX.y + 0.08, ICE_BOX.z + FROST_SPILL, ICE_BOX.w + FROST_SPILL);
    float below = softBox(uv, spill, 0.03)
                * (1.0 - smoothstep(ICE_BOX.w, spill.w, uv.y) * 0.9)          // thins as it sinks
                * (1.0 - smoothstep(ICE_BOX.z, spill.z, uv.x) * 0.9);         // ...and as it drifts off
    return max(onIce, below * 0.6);
}

// Cold wisps sinking down and to the right.
float vapour(vec2 uv, float t) {
    vec2  q = vec2(uv.x * u_aspect / FROST_STRETCH - t * FROST_SPEED, uv.y - t * FROST_SPEED * 0.6) * FROST_SCALE;
    float n = valueNoise(q) * 0.6 + valueNoise(q * 2.3 + vec2(0.0, t * 0.15)) * 0.4;
    return smoothstep(0.38, 0.72, n);
}

// One ice mote: drifts down-right off the shelf, glinting on its own beat.
float mote(vec2 q, float i, float t) {
    float r0 = hash11(i * 3.3 + 0.9), r1 = hash11(i * 5.1 + 2.7);
    float r2 = hash11(i * 6.9 + 4.1), r3 = hash11(i * 8.3 + 6.3);
    float life  = mod(t / MOTE_LIFE + r2, 1.0);
    vec2  start = vec2(mix(ICE_BOX.x, ICE_BOX.z, r0), mix(ICE_BOX.y + 0.06, ICE_BOX.w, r1));
    vec2  pos   = start + vec2(MOTE_DRIFT, MOTE_SINK) * life;
    vec2  p     = q - vec2(pos.x * u_aspect, pos.y);
    float dot   = smoothstep(MOTE_SIZE, MOTE_SIZE * 0.3, length(p));
    float glint = 0.35 + 0.65 * pow(0.5 + 0.5 * sin(t * MOTE_TWINKLE * (0.7 + 0.6 * r3) + r3 * 6.28318), 4.0);
    float alive = smoothstep(0.0, 0.15, life) * smoothstep(1.0, 0.7, life);
    return dot * glint * alive;
}

// 1 just under an ice edge, fading to 0 FOG_DROP below it â€” follows the shelf's
// real silhouette by looking for ice a little way up.
float underLip(vec3 here, vec2 uv) {
    // Sampled through the painting's mipmaps so the edge test is soft, not banded.
    float sum = 0.0, wsum = 0.0;
    for (int i = 1; i <= 4; i++) {
        float d   = FOG_DROP * float(i) / 4.0;
        float w   = 1.0 - d / FOG_DROP * 0.75;                // ice close above counts most
        sum  += w * iceKey(texture2D(u_texture, uv - vec2(0.0, d), FOG_BLUR).rgb);
        wsum += w;
    }
    float under = smoothstep(0.08, 0.6, sum / wsum);
    return under * mix(1.0, FOG_ON_ICE, iceKey(here)) * softBox(uv, LEDGE_BOX, 0.03);
}

// Fog pouring over the lip: dense at the edge, billowing down, gone FOG_DROP below.
float fogFall(vec3 here, vec2 uv, float t) {
    vec2  q     = vec2(uv.x * u_aspect, uv.y - t * FOG_SPEED) * FOG_SCALE;
    float n     = valueNoise(q) * 0.55 + valueNoise(q * 2.2 + vec2(t * 0.1, -t * 0.4)) * 0.45;
    float body  = smoothstep(0.30, 0.65, n);
    return mix(0.35, 1.0, body) * underLip(here, uv);
}

vec3 frost(vec3 col, vec3 still, vec2 uv, float t) {
    vec2  q = vec2(uv.x * u_aspect, uv.y);
    float wisps = vapour(uv, t) * frostRegion(still, uv) * FROST_LEVEL;
    float fall  = fogFall(still, uv, t) * FOG_LEVEL;
    col = mix(col, FOG_TINT, clamp(fall, 0.0, 1.0) * u_intensity);
    float motes = 0.0;
    for (float i = 0.0; i < MOTE_COUNT; i += 1.0) motes += mote(q, i, t);
    col = mix(col, FROST_TINT, clamp(wisps, 0.0, 1.0) * u_intensity);
    col = mix(col, MOTE_TINT,  clamp(motes, 0.0, 1.0) * u_intensity);
    return col;
}

// ---- lightning -------------------------------------------------------------

// Seconds since this slot's strike began; negative before it, and never fires
// in an empty slot.
float strikeAge(float t, float slot) {
    float local = fract(t / STRIKE_PERIOD) * STRIKE_PERIOD;
    float fires = step(STRIKE_RARITY, hash11(slot));
    float at    = 0.3 + (STRIKE_PERIOD - 1.2) * hash11(slot + 31.0);
    return mix(-1.0, local - at, fires);
}

// Brightness of a strike `age` seconds in: hard on, stuttering as it dies.
float strikeEnvelope(float age, float slot) {
    float body  = exp(-max(age - STRIKE_LEAD, 0.0) * STRIKE_DECAY) * step(0.0, age);
    float crack = step(0.35, hash11(floor(age * CRACKLE_HZ) + slot * 7.0));
    return body * mix(CRACKLE_FLOOR, 1.0, crack);
}

// Zigzag offset in -0.5..0.5, sharp-cornered.
float kink(float k, float nodes, float seed) {
    float i = floor(k * nodes);
    return mix(hash11(i + seed), hash11(i + 1.0 + seed), fract(k * nodes)) - 0.5;
}

// 1 where the bolt has already run to, 0 below the leader's tip.
float leaderReach(vec2 uv, float age) {
    float tip = mix(BOLT_TOP, BOLT_BOTTOM, clamp(age / STRIKE_LEAD, 0.0, 1.0));
    return smoothstep(tip + 0.01, tip - 0.01, uv.y);
}

// Sideways wobble of a jagged path at parameter k (0 = top, 1 = bottom).
float jag(float k, float seed) {
    return BOLT_KINK_AMP * kink(k, BOLT_KINKS, seed) + BOLT_JITTER * kink(k, BOLT_KINKS * 3.0, seed + 9.0);
}

// x of the path that starts at x0 on y0, ends at y1, drifting `lean` on the way.
float pathX(float k, float x0, float lean, float seed) {
    return x0 + lean * k + jag(clamp(k, 0.0, 1.0), seed);
}

// Aspect-corrected distance to that path: horizontal along its run, and to the
// nearest end point past either end, so the glow rounds off instead of stopping dead.
float pathDistance(vec2 uv, float x0, float y0, float y1, float lean, float seed) {
    float k  = (uv.y - y0) / max(y1 - y0, 1e-4);
    float dx = (uv.x - pathX(k, x0, lean, seed)) * u_aspect;
    float dy = (k - clamp(k, 0.0, 1.0)) * (y1 - y0);
    return length(vec2(dx, dy));
}

// The whole strike shape for `slot`: core mask, glow, and where it hits.
// Returns vec3(core, glow, splash).
vec3 boltShape(vec2 uv, float slot) {
    float x0   = BOLT_X + (hash11(slot * 1.3 + 2.0) - 0.5) * 2.0 * BOLT_WANDER;
    float lean = (hash11(slot * 2.1 + 5.0) - 0.5) * 2.0 * BOLT_LEAN;
    float dT   = pathDistance(uv, x0, BOLT_TOP, BOLT_BOTTOM, lean, slot);
    float core = smoothstep(BOLT_WIDTH, 0.0, dT);
    float glow = exp(-dT / BOLT_HALO) + 0.3 * exp(-dT / (BOLT_HALO * HALO_TAIL));   // tight glow plus a long soft tail
    for (int i = 0; i < 3; i++) {
        float fi   = float(i);
        float k0   = 0.15 + 0.6 * hash11(slot * 3.7 + fi * 11.0);          // where on the trunk it forks
        float ys   = mix(BOLT_TOP, BOLT_BOTTOM, k0);
        float xs   = pathX(k0, x0, lean, slot);
        float ye   = ys + BRANCH_LEN * (BOLT_BOTTOM - BOLT_TOP) * (0.5 + 0.5 * hash11(slot * 4.3 + fi * 13.0));
        float bl   = (hash11(slot * 5.9 + fi * 17.0) - 0.5) * 2.0 * BRANCH_LEAN;
        float dB   = pathDistance(uv, xs, ys, ye, bl, slot * 7.0 + fi * 19.0);
        core = max(core, smoothstep(BOLT_WIDTH * BRANCH_WIDTH, 0.0, dB));
        glow = max(glow, 0.6 * exp(-dB / BOLT_HALO) + 0.2 * exp(-dB / (BOLT_HALO * HALO_TAIL)));
    }
    vec2  hit    = vec2(pathX(1.0, x0, lean, slot), BOLT_BOTTOM);
    float splash = exp(-length((uv - hit) * vec2(u_aspect, 1.0)) / SPLASH_REACH);
    return vec3(core, glow, splash);
}

// One short purple fork snapping through the bank: a zigzag from A along `dir`.
float crackleFork(vec2 uv, float i, float t) {
    float slot  = floor(t / CRACKLE_PERIOD + i * 0.41);
    float local = fract(t / CRACKLE_PERIOD + i * 0.41) * CRACKLE_PERIOD;
    float fires = step(CRACKLE_RARITY, hash11(slot * 1.9 + i * 17.0));
    float at    = 0.1 + (CRACKLE_PERIOD - 0.5) * hash11(slot * 2.7 + i * 23.0);
    float age   = local - at;
    float env   = exp(-max(age, 0.0) * CRACKLE_DECAY) * step(0.0, age) * fires
                * mix(0.4, 1.0, step(0.4, hash11(floor(age * 40.0) + slot)));   // stutter
    vec2  a     = vec2(mix(STORM_CLOUDS.x + 0.04, STORM_CLOUDS.z - 0.06, hash11(slot * 3.3 + i * 31.0)),
                       mix(STORM_CLOUDS.y + 0.04, STORM_CLOUDS.w - 0.06, hash11(slot * 4.1 + i * 37.0)));
    float th    = hash11(slot * 5.9 + i * 43.0) * 6.28318;
    vec2  dir   = vec2(cos(th) / u_aspect, sin(th));
    vec2  nrm   = vec2(-dir.y * u_aspect * u_aspect, dir.x) / u_aspect;
    vec2  rel   = uv - a;
    float k     = clamp(dot(rel * vec2(u_aspect, 1.0), dir * vec2(u_aspect, 1.0)) / CRACKLE_LEN, 0.0, 1.0);
    vec2  onLine = a + dir * CRACKLE_LEN * k + nrm * CRACKLE_KINK * kink(k, 6.0, slot + i * 7.0);
    float dist  = length((uv - onLine) * vec2(u_aspect, 1.0));
    float taper = smoothstep(1.0, 0.7, k);
    float core  = smoothstep(CRACKLE_WIDTH, 0.0, dist) * taper;
    float glow  = exp(-dist / CRACKLE_HALO) * taper;
    return (core * 2.0 + glow) * env;
}

// Three sheet flashes on their own slots, each a soft blob somewhere in the bank.
float sheetFlashes(vec2 uv, float t) {
    float sum = 0.0;
    for (int i = 0; i < 3; i++) {
        float fi    = float(i);
        float slot  = floor(t / SHEET_PERIOD + fi * 0.37);
        float local = fract(t / SHEET_PERIOD + fi * 0.37) * SHEET_PERIOD;
        float fires = step(SHEET_RARITY, hash11(slot * 1.7 + fi * 13.0));
        float at    = 0.2 + (SHEET_PERIOD - 0.6) * hash11(slot * 2.3 + fi * 29.0);
        float age   = local - at;
        float env   = exp(-max(age, 0.0) * SHEET_DECAY) * step(0.0, age) * fires;
        vec2  pos   = vec2(mix(STORM_CLOUDS.x + 0.03, STORM_CLOUDS.z - 0.03, hash11(slot * 3.1 + fi * 41.0)),
                           mix(STORM_CLOUDS.y + 0.03, STORM_CLOUDS.w - 0.06, hash11(slot * 4.7 + fi * 53.0)));
        float d     = length((uv - pos) * vec2(u_aspect, 1.0));
        sum += exp(-d / SHEET_REACH) * env;
    }
    return sum;
}

vec3 lightning(vec3 col, vec3 still, vec2 uv, float t) {
    float slot = floor(t / STRIKE_PERIOD);
    float age  = strikeAge(t, slot);
    float live = strikeEnvelope(age, slot) * u_intensity;

    // Sheet lightning in the bank, and the cloud brightening just before the bolt.
    float bank  = max(stormKey(still, uv), 0.35 * softBox(uv, STORM_CLOUDS, 0.04));
    float pre   = smoothstep(-PRE_FLASH, -0.03, age) * (1.0 - step(0.0, age))
                * exp(-length((uv - vec2(BOLT_ORIGIN.x, STORM_BOX.y)) * vec2(u_aspect, 1.0)) / 0.12);
    float sheet = sheetFlashes(uv, t) * SHEET_GAIN + pre * PRE_GAIN;
    col += SHEET_TINT * sheet * bank * u_intensity;

    // Small purple forks crackling through the bank.
    float forks = 0.0;
    for (float i = 0.0; i < 4.0; i += 1.0) forks += crackleFork(uv, i, t);
    float inBank = max(stormKey(still, uv), 0.5 * softBox(uv, STORM_CLOUDS, 0.02));
    col = mix(col, CRACKLE_GLOW, clamp(forks * 0.9, 0.0, 1.0) * inBank * u_intensity);
    col = mix(col, CRACKLE_CORE, clamp(forks - 1.0, 0.0, 1.0) * inBank * u_intensity);
    vec3  shape = boltShape(uv, slot);
    float here  = leaderReach(uv, age) * live;

    // Strike: flash the neighbourhood, glow around the bolt, splash where it hits, then the white core.
    float d      = length((uv - BOLT_ORIGIN) * vec2(u_aspect, 1.0));
    float flash  = exp(-d / FLASH_REACH) * live * FLASH_GAIN;
    float halo   = shape.y * here * HALO_GAIN;
    float splash = shape.z * live * SPLASH_GAIN * step(STRIKE_LEAD, age);   // only once the leader has landed
    // Soft knee (1 - e^-x) instead of a clamp, so the light fades to nothing without a plateau edge.
    col = mix(col, BOLT_GLOW, 1.0 - exp(-flash));
    col = mix(col, BOLT_GLOW, 1.0 - exp(-(halo + splash)));
    col = mix(col, BOLT_CORE, shape.x * here);
    return col;
}

// ---- stars -----------------------------------------------------------------

// Brightness multiplier that breathes on a phase drawn from a smooth field, so
// each star has its own rhythm without needing an id.
float twinkle(vec2 q, float t) {
    float phase = valueNoise(q * TWINKLE_GRAIN) * 6.28318 * 3.0;
    float rate  = 0.7 + 0.6 * valueNoise(q * TWINKLE_GRAIN + 17.0);
    return 0.5 + 0.5 * sin(t * TWINKLE_SPEED * rate * 6.28318 + phase);
}

vec3 stars(vec3 col, vec2 uv, float t) {
    vec2  q     = vec2(uv.x * u_aspect, uv.y);
    vec4  layer = texture2D(u_stars, uv);
    float level = mix(TWINKLE_DIM, 1.0 + TWINKLE_FLARE, twinkle(q, t));
    col = mix(col, layer.rgb, layer.a * min(level, 1.0) * u_intensity);
    return col + STAR_TINT * layer.a * max(level - 1.0, 0.0) * u_intensity;
}

// ---- tower -----------------------------------------------------------------

// How much of this pixel is a ring: lavender-bright inside the box (the white
// spire and the blue sky both fail the colour tests).
float towerKey(vec3 c, vec2 uv) {
    float lum = (c.r + c.g + c.b) / 3.0;
    return smoothstep(RING_KEY_LO, RING_KEY_HI, c.b - c.g) * smoothstep(-0.03, 0.05, c.r - c.g)
         * smoothstep(0.35, 0.55, lum) * softBox(uv, TOWER_BOX, 0.03);
}

vec3 tower(vec3 col, vec3 still, vec2 uv, float t) {
    float breathe = 1.0 + TOWER_BREATHE * sin(t * 0.7);
    float rings   = towerKey(still, uv) * RING_GLOW;
    float tip     = exp(-length((uv - TIP_POS) * vec2(u_aspect, 1.0)) / TIP_REACH) * TIP_GAIN;
    col += TOWER_TINT * rings * breathe * u_intensity;
    col += TIP_TINT   * tip   * breathe * u_intensity;
    return col;
}

// ---- wind ------------------------------------------------------------------
// Everything here works in `q` = aspect-corrected uv, so streaks and leaves
// keep their shape on any window.

// Where lane `r` (0..1) begins: just past the path's right edge, in aspect-corrected units.
float laneStart(float r) {
    return mix(WIND_START.x, WIND_START.y, r) * u_aspect;
}

// Height of lane `r` (0..1) at position x: a straight run that sags across the band.
float laneY(float r, float x, float x0) {
    return mix(WIND_BAND.y, WIND_BAND.w, r) + (x - x0) * WIND_DESCENT;
}

// How much of this pixel is foliage the wind can pass behind: green on top and
// clearly above blue, inside the band â€” cliffs, path and pools stay out.
float canopy(vec3 c, vec2 uv) {
    return smoothstep(CANOPY_KEY_LO, CANOPY_KEY_HI, c.g - max(c.r, c.b))
         * smoothstep(0.02, 0.08, c.g - c.b)
         * softBox(uv, WIND_BAND, 0.0);
}

// The cave mouth on the right: everything past its diagonal silhouette, plus any
// dark rock near it, which the wind slips behind.
float caveWall(vec3 c, vec2 uv) {
    float edge = CAVE_WALL_EDGE.x + CAVE_WALL_EDGE.y * uv.y;
    float past = smoothstep(edge - 0.006, edge + 0.006, uv.x);
    float lum  = (c.r + c.g + c.b) / 3.0;
    float dark = smoothstep(CAVE_WALL_LUM + 0.05, CAVE_WALL_LUM - 0.02, lum) * smoothstep(CAVE_WALL_X, CAVE_WALL_X + 0.03, uv.x);
    return max(past, dark);
}

// Per-lane depth cues: near lanes are bigger, faster and brighter, and foliage
// hides less of them.
float depthScale(float lane) { return mix(WIND_FAR_SCALE, 1.0, lane); }
float depthAlpha(float lane) { return mix(WIND_FAR_ALPHA, 1.0, lane); }
float depthShow(float lane, float leaves) { return 1.0 - leaves * mix(WIND_HIDE_FAR, WIND_HIDE_NEAR, lane); }

// One streak: a bright S-curved dash whose head runs the band, then rests.
float streak(vec2 q, float i, float t, float leaves) {
    float r0 = hash11(i * 3.1 + 0.7), r1 = hash11(i * 5.3 + 1.9), r2 = hash11(i * 7.7 + 4.2);
    float x0 = laneStart(r0), x1 = WIND_BAND.z * u_aspect;
    float len    = STREAK_LEN * depthScale(r0);
    float speed  = STREAK_SPEED * depthScale(r0);
    float travel = (x1 - x0 + len) / speed;
    float period = travel + STREAK_GAP * (0.6 + 0.8 * r2);
    float head   = x0 + mod(t + r1 * period, period) * speed;
    float s      = (q.x - (head - len)) / len;   // 0 at tail, 1 at head
    float along  = smoothstep(0.0, 0.55, s) * smoothstep(1.0, 0.80, s);   // pointed tail, blunt head
    float yc     = laneY(r0, q.x, x0) + STREAK_WAVE * sin(q.x * STREAK_WAVES + r1 * 6.28318);
    float d      = abs(q.y - yc);
    float width  = STREAK_WIDTH * along * depthScale(r0);
    float body   = smoothstep(width, 0.0, d)
                 + 0.35 * smoothstep(width * 3.0, 0.0, d);   // soft glow around the core
    float inBand = smoothstep(x0, x0 + 0.06, q.x) * step(q.x, x1);   // emerges from the treeline
    return body * along * inBand * depthAlpha(r0) * depthShow(r0, leaves);
}

// One leaf: a tumbling ellipse blown down a lane, bobbing as it goes.
float leaf(vec2 q, float i, float t, float leaves) {
    float r0 = hash11(i * 2.3 + 0.3), r1 = hash11(i * 4.1 + 2.2);
    float r2 = hash11(i * 6.7 + 5.1), r3 = hash11(i * 8.9 + 7.4);
    float x0 = laneStart(r0), x1 = WIND_BAND.z * u_aspect;
    float speed = mix(LEAF_SPEED_MIN, LEAF_SPEED_MAX, r1) * depthScale(r0);
    float size  = LEAF_SIZE * depthScale(r0);
    float x     = x0 + mod(t * speed + r2 * (x1 - x0), x1 - x0);
    float y     = laneY(r0, x, x0) + LEAF_BOB * sin(t * 2.4 + r3 * 6.28318);
    float a     = t * LEAF_TUMBLE * (0.6 + 0.8 * r3) + r2 * 6.28318;
    vec2  p     = q - vec2(x, y);
    p = vec2(p.x * cos(a) - p.y * sin(a), p.x * sin(a) + p.y * cos(a));
    float d     = length(p / vec2(size, size * 0.45)) - 1.0;
    float fade  = smoothstep(x0, x0 + 0.08, x);
    return smoothstep(0.35, 0.0, d) * fade * depthAlpha(r0) * depthShow(r0, leaves);
}

vec3 wind(vec3 col, vec3 still, vec2 uv, float t) {
    vec2  q      = vec2(uv.x * u_aspect, uv.y);
    float leaves = canopy(still, uv);
    float wall   = caveWall(still, uv);
    float streaks = 0.0;
    for (float i = 0.0; i < STREAK_COUNT; i += 1.0) streaks += streak(q, i, t, leaves);
    float leavesA = 0.0, leavesB = 0.0;
    for (float i = 0.0; i < LEAF_COUNT; i += 1.0) {
        float l = leaf(q, i, t, leaves);
        leavesA += l * step(0.5, hash11(i * 1.7));
        leavesB += l * step(hash11(i * 1.7), 0.5);
    }
    float show = (1.0 - wall) * u_intensity;
    col = mix(col, STREAK_TINT, clamp(streaks * STREAK_ALPHA, 0.0, 1.0) * show);
    col = mix(col, LEAF_TINT_A, clamp(leavesA, 0.0, 1.0) * show);
    col = mix(col, LEAF_TINT_B, clamp(leavesB, 0.0, 1.0) * show);
    return col;
}

void main() {
    vec2  uv = v_texCoords;
    float t  = u_time;
    vec2  q  = vec2(uv.x * u_aspect, uv.y) * RIPPLE_SCALE;

    vec3  still = texture2D(u_texture, uv).rgb;
    vec3  col   = clouds(still, still, uv, t);
    float wet   = wetness(still, uv) * u_intensity;

    // Water: bend what's under the surface, light the slopes, sparkle the crests.
    vec2  slope   = surfaceSlope(q, t);
    vec3  bent    = texture2D(u_texture, uv + slope * RIPPLE_REFRACT * wet).rgb;
    float shade   = 1.0 + slope.y * RIPPLE_SHADE * wet;
    float sparkle = glints(q, t) * wet;
    col = mix(col, bent * shade, wet);
    col = mix(col, GLINT_TINT, sparkle);

    // Falls: foam ribs dropping down the hand-placed strips.
    float foam = fallFoam(uv, t) * fallMask(uv) * wet;
    col = mix(col, FOAM_TINT, foam);

    col = fire(col, still, uv, t);
    col = dust(col, still, uv, t);
    col = frost(col, still, uv, t);
    col = tower(col, still, uv, t);
    col = wind(col, still, uv, t);
    col = lightning(col, still, uv, t);
    col = stars(col, uv, t);

    gl_FragColor = vec4(col * v_color.rgb, 1.0);
}
