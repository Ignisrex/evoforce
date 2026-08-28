// Dark blast: a void-black orb that swallows what's behind it, wrapped in a
// torn violet rim with shadow tendrils spiralling in toward the core.
// Blend: GL_ONE, GL_ONE_MINUS_SRC_ALPHA (premultiplied alpha) — the core
// darkens the scene through alpha, the rim and tendrils add violet light.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_texture;  // 1x1 white pixel; sampled to keep SpriteBatch happy
uniform float u_time;         // seconds since the projectile spawned
uniform float u_dir;          // +1 flying right (player shot), -1 flying left (enemy)
uniform vec3  u_tint;         // skill vfxTint; tints the rim/tendrils, not the void

// ---- tuning ----
const float CORE_RADIUS    = 0.22;  // void core radius; the quad spans -1..1
const float CORE_SOFT      = 0.05;  // softness of the core's edge
const float RIM_WIDTH      = 0.08;  // violet rim half-thickness
const float RIM_RAGGED     = 0.08;  // how much the noise tears the rim
const float RIM_BOOST      = 1.8;   // rim brightness; >1 blooms
const float FLICKER_RATE   = 10.0;  // rim shimmer, cycles per second
const float FLICKER_DEPTH  = 0.5;   // 0 = steady ring, 1 = strobing
const float SWIRL          = 5.0;   // spiral shear, radians across the quad radius
const float SPIN_SPEED     = 6.0;   // rotation, radians per second
const float NOISE_SCALE    = 3.5;   // tendril grain; higher = finer filaments
const float TENDRIL_THRESH = 0.58;  // noise level a tendril needs to show
const float TENDRIL_REACH  = 0.90;  // how far tendrils extend from the center
const float HAZE_REACH     = 0.75;  // how far the soft violet halo carries
const float HAZE_LEVEL     = 0.30;  // halo brightness at the rim
const float SPAWN_TIME     = 0.12;  // seconds to swell in from nothing
const float OCCLUSION      = 0.90;  // how completely the core blots out the scene
const vec3  CORE_COLOR     = vec3(0.02, 0.00, 0.05);  // near-black — void, not a render hole
const vec3  RIM_COLOR      = vec3(0.60, 0.25, 1.00);
const vec3  TENDRIL_COLOR  = vec3(0.35, 0.12, 0.60);

// ---- helpers ----

// 2D -> 1D hash in 0..1, mediump-safe.
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
    return mix(mix(hash21(i),                   hash21(i + vec2(1.0, 0.0)), u.x),
               mix(hash21(i + vec2(0.0, 1.0)),  hash21(i + vec2(1.0, 1.0)), u.x), u.y);
}

// Rotating each octave keeps the noise lattice from streaking.
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

mat2 rot(float a) {
    float s = sin(a), c = cos(a);
    return mat2(c, -s, s, c);
}

// Shadowstuff spiralling inward: the sample frame twists more with radius, so
// plain noise reads as arms being dragged around and into the core. Rotating
// the frame (instead of unwrapping to polar) avoids the atan seam.
float voidField(vec2 p, float r, float time, float dir) {
    float twist = r * SWIRL - dir * time * SPIN_SPEED;
    return fbm(rot(twist) * p * NOISE_SCALE);
}

void main() {
    vec2 p = (v_texCoords - 0.5) * 2.0;              // centered; orb at the origin

    // Swell in from nothing instead of popping onto the screen.
    float grown = smoothstep(0.0, SPAWN_TIME, u_time);
    p /= max(grown, 0.05);
    float r = length(p);

    float n = voidField(p, r, u_time, u_dir);        // spiralling shadow, 0..1

    // The void core: darkens the scene through alpha, emits almost nothing.
    float core = 1.0 - smoothstep(CORE_RADIUS - CORE_SOFT, CORE_RADIUS + CORE_SOFT, r);

    // Violet rim hugging the core's edge, torn by the spiralling noise and
    // guttering fast — a barely-contained ring, not a steady lamp.
    float rim = 1.0 - smoothstep(0.0, RIM_WIDTH, abs(r - CORE_RADIUS) - RIM_RAGGED * (n - 0.5));
    float rimPulse = 1.0 - FLICKER_DEPTH * valueNoise(vec2(u_time * FLICKER_RATE, 1.3));

    // Tendrils: bright noise filaments outside the core, dying with radius.
    float outside = smoothstep(CORE_RADIUS * 0.8, CORE_RADIUS * 1.1, r);
    float fade    = 1.0 - smoothstep(CORE_RADIUS, TENDRIL_REACH, r);
    float tendril = smoothstep(TENDRIL_THRESH, TENDRIL_THRESH + 0.2, n) * outside * fade;

    // Soft violet halo leaking past the rim — bloom food.
    float haze = HAZE_LEVEL * (1.0 - smoothstep(CORE_RADIUS, HAZE_REACH, r)) * outside;

    vec3  light = (RIM_COLOR * (rim * RIM_BOOST * rimPulse + haze)
                   + TENDRIL_COLOR * tendril) * u_tint;
    float occ   = min(OCCLUSION * core + 0.25 * tendril, 1.0);  // tendrils shade a little too
    vec3  rgb   = CORE_COLOR * core + light;

    vec4 texel = texture2D(u_texture, v_texCoords);  // white pixel — keeps the sampler live
    gl_FragColor = vec4(rgb * grown, occ * grown) * texel;
}
