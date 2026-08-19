// Celestial veil: a deep night sky — starfield and drifting nebula haze —
// with aurora curtains hanging from above, their folds sliding downward, and a
// broad radiance behind the middle of the screen so the reward cards sit in
// light. Fully procedural; the sampler is a 1x1 white pixel only because there
// is nothing here worth authoring as art.
// Blend: none — this is an opaque background pass.

#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;  // 1x1 white pixel; SpriteBatch needs something bound
uniform float u_time;         // seconds, free-running
uniform float u_aspect;       // width / height, so stars and glows stay round
uniform vec3  u_skyTop;       // zenith colour
uniform vec3  u_skyBottom;    // horizon colour
uniform vec3  u_nebula;       // faint haze tint behind the curtains
uniform vec3  u_veilA;        // near curtain (teal in the default palette)
uniform vec3  u_veilB;        // far curtain (violet)
uniform vec3  u_radiance;     // glow behind the reward cards
uniform float u_intensity;    // master multiplier on the curtains, 0 = sky only

// ---- tuning ----
const float WARP_STRENGTH  = 0.22;  // how far noise bends the curtains, 0 = clean sine waves
const float SWAY           = 0.10;  // horizontal wander of a curtain, in screen heights
const float FOLD_SCALE     = 9.0;   // striations across a curtain; higher = finer folds
const float FOLD_SPEED     = 0.45;  // how fast folds slide down the curtain
const float CURTAIN_BASE   = 0.18;  // height where curtains ignite, 0 = bottom of screen
const float CURTAIN_TOP    = 1.15;  // height where they have faded out entirely
const float NEAR_X         = 0.34;  // near curtain's resting position, fraction of width
const float NEAR_WIDTH     = 0.16;  // its half-width, in screen heights
const float FAR_X          = 0.68;  // far curtain's resting position
const float FAR_WIDTH      = 0.11;
const float FAR_DIM        = 0.65;  // far curtain is dimmer, which reads as depth
const float STAR_CELLS     = 90.0;  // star grid density; higher = smaller, denser points
const float STAR_RARITY    = 0.92;  // fraction of cells left empty
const float STAR_SIZE_MIN  = 0.06;  // dimmest star's radius, in cell units
const float STAR_SIZE_MAX  = 0.16;  // brightest star's radius
const float STAR_DRIFT     = 0.002; // slow vertical wheel of the sky
const float NEBULA_SCALE   = 1.4;   // haze feature size; higher = smaller wisps
const float NEBULA_WARP    = 0.6;   // how much the haze folds back on itself
const float NEBULA_LEVEL   = 0.35;  // haze brightness
const float RADIANCE_Y     = 0.46;  // vertical centre of the glow behind the cards
const float RADIANCE_SIZE  = 0.62;  // its reach, in screen heights
const float RADIANCE_LEVEL = 0.55;  // its brightness; 0 removes the framing glow
const float VIGNETTE       = 0.30;  // corner darkening, 0 = flat

// ---- helpers ----

// 2D -> 1D hash in 0..1.
float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// Smooth value noise in 0..1, one cell per unit of p.
float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i + vec2(0.0, 0.0)), hash21(i + vec2(1.0, 0.0)), u.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), u.x), u.y);
}

// Rotating each octave keeps the noise lattices from lining up, which is what
// produces grid-shaped streaks across a large smooth field.
const mat2 FBM_ROT = mat2(0.80, 0.60, -0.60, 0.80);

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amp * valueNoise(p);
        p    = FBM_ROT * p * 2.03 + vec2(17.0, 9.0);
        amp *= 0.5;
    }
    return sum;
}

// Pinpoint stars on a jittered grid, each twinkling on its own cycle. The
// rarity term doubles as a brightness ramp, so the faintest stars fade in
// instead of popping into existence at the threshold.
float starField(vec2 p, float time) {
    vec2  cell     = floor(p);
    vec2  local    = fract(p) - 0.5;
    float seed     = hash21(cell);
    float presence = max(seed - STAR_RARITY, 0.0) / (1.0 - STAR_RARITY);
    vec2  jitter   = vec2(hash21(cell + 1.7), hash21(cell + 3.1)) - 0.5;
    float dist     = length(local - jitter * 0.6);
    float size     = mix(STAR_SIZE_MIN, STAR_SIZE_MAX, hash21(cell + 5.3));
    float twinkle  = 0.6 + 0.4 * sin(time * (1.5 + seed * 3.0) + seed * 40.0);
    return smoothstep(size, 0.0, dist) * twinkle * presence;
}

// Wispy cloud haze: fbm warped by its own fbm, drifting slowly sideways.
float nebulaHaze(vec2 p, float time) {
    vec2  hp = p * NEBULA_SCALE + vec2(time * 0.02, -time * 0.01);
    float n  = fbm(hp + NEBULA_WARP * (fbm(hp * 0.7 + 3.1) - 0.5));
    return smoothstep(0.35, 0.85, n);
}

// Domain warp: displaces the whole sampling point by slow noise so the
// curtains bend and swell instead of tracing clean sine waves.
vec2 breathe(vec2 p, float time, float strength) {
    vec2 offset = vec2(fbm(p * 0.8 + vec2(0.0, time * 0.06)),
                       fbm(p * 0.8 + vec2(4.7, 2.1 - time * 0.05)));
    return p + strength * (offset - 0.5);
}

// Horizontal distance from a curtain's wavering spine. Two out-of-step waves
// so the sway never resolves into an obvious repeat.
float spineOffset(vec2 p, float baseX, float phase, float time) {
    float drift = SWAY * sin(p.y * 1.8 + phase + time * 0.35)
                + SWAY * 0.5 * sin(p.y * 3.7 - phase + time * 0.22);
    return p.x - (baseX + drift);
}

// Bright core with a wide faint halo. Super-gaussian, so the body reads as a
// solid sheet of light with a firm edge rather than a soft blur.
float sheetProfile(float offset, float halfWidth) {
    float x  = offset / halfWidth;
    float x2 = x * x;
    return 0.75 * exp(-x2 * x2 * 2.2) + 0.25 * exp(-x2 * 0.8);
}

// Real aurora ignites at a lower edge and thins out with altitude.
float heightFade(float y) {
    float ignite = smoothstep(CURTAIN_BASE - 0.18, CURTAIN_BASE + 0.10, y);
    float thin   = 1.0 - smoothstep(CURTAIN_BASE, CURTAIN_TOP, y);
    return ignite * thin;
}

// Vertical striations sliding down the sheet — the "curtain" read.
float foldPattern(vec2 p, float time) {
    float n = fbm(vec2(p.x * FOLD_SCALE, p.y * 0.8 - time * FOLD_SPEED));
    return 0.45 + 0.55 * smoothstep(0.25, 0.75, n);
}

// One curtain's brightness at p. Everything above, composed.
float curtain(vec2 p, float baseX, float halfWidth, float phase, float time) {
    float offset = spineOffset(p, baseX, phase, time);
    return sheetProfile(offset, halfWidth) * heightFade(p.y) * foldPattern(p, time);
}

// Broad soft glow behind the middle of the screen, so the cards sit in light
// instead of competing with the sky.
float radianceField(vec2 p, float centreX) {
    vec2 d = (p - vec2(centreX, RADIANCE_Y)) / RADIANCE_SIZE;
    return exp(-dot(d, d) * 1.6);
}

// Soft-clip the additive stack (ACES fit) so overlapping bright layers roll
// off toward white instead of flattening into one saturated slab.
vec3 tonemap(vec3 c) {
    return clamp((c * (2.51 * c + 0.03)) / (c * (2.43 * c + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    // SpriteBatch quads have v = 0 at the top; flip so y grows upward.
    vec2 uv = vec2(v_texCoords.x, 1.0 - v_texCoords.y);
    vec2 p  = vec2(uv.x * u_aspect, uv.y);      // aspect-corrected: y spans 0..1
    vec2 warped = breathe(p, u_time, WARP_STRENGTH);

    vec3  sky      = mix(u_skyBottom, u_skyTop, smoothstep(0.0, 1.0, uv.y));
    float haze     = nebulaHaze(p, u_time);
    float stars    = starField((p + vec2(0.0, u_time * STAR_DRIFT)) * STAR_CELLS, u_time);
    float radiance = radianceField(p, 0.5 * u_aspect);

    float nearSheet = curtain(warped, NEAR_X * u_aspect, NEAR_WIDTH, 0.0, u_time);
    float farSheet  = curtain(warped, FAR_X  * u_aspect, FAR_WIDTH,  2.7, u_time) * FAR_DIM;

    vec3 veil = u_veilA * nearSheet + u_veilB * farSheet;

    vec3 col = sky;
    col += u_nebula * haze * NEBULA_LEVEL;
    col += u_radiance * radiance * RADIANCE_LEVEL;
    col += vec3(stars);
    col += veil * u_intensity;

    col  = tonemap(col);
    col *= mix(1.0 - VIGNETTE, 1.0, smoothstep(1.4, 0.4, length(uv - 0.5)));

    gl_FragColor = vec4(col, 1.0) * v_color * texture2D(u_texture, v_texCoords);
}
