// Frost Trap: a sheet of ice frozen onto the floor tile — pixel-quantized so it
// sits with the PixelLab sprites rather than reading as a smooth decal. A
// ragged-edged slab in a few flat ice tones, veined with dark cracks, with a
// slow highlight sweep so it looks glassy rather than painted on.
// Blend: GL_ONE, GL_ONE_MINUS_SRC_ALPHA (premultiplied) — rgb is the lit ice,
// alpha is how much of the floor the slab covers.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_texture;  // 1x1 white pixel; sampled to keep SpriteBatch happy
uniform float u_time;         // seconds since cast
uniform float u_dir;          // unused: the slab is symmetric
uniform vec3  u_tint;         // skill tint; white = the blues below
uniform float u_alpha;        // phase envelope 0..1 (freeze in, thaw out)

// ---- tuning ----
const float CELLS       = 28.0;   // pixel grid across the quad
const float EDGE_RAGGED = 0.18;   // how far the edge wanders in/out (in radius units)
const float EDGE_SCALE  = 5.0;    // ragged-edge noise frequency around the rim
const float CRACK_SCALE = 3.5;    // cells per quad for the crack network
const float CRACK_WIDTH = 0.045;  // crack thickness (in cell units)
const float CRACK_LEVEL = 0.55;   // how much a crack darkens the ice
const float SHEEN_RATE  = 0.35;   // highlight sweeps per second
const float SHEEN_WIDTH = 0.18;   // width of the sweep band
const float SHEEN_LEVEL = 0.30;   // brightness it adds
const float COVER       = 0.70;   // how much the slab hides the floor (alpha)
const vec3 TONE0 = vec3(0.36, 0.58, 0.86);   // deep ice, near the cracks
const vec3 TONE1 = vec3(0.55, 0.76, 0.95);   // body
const vec3 TONE2 = vec3(0.74, 0.88, 1.00);   // lit facets
const vec3 TONE3 = vec3(0.92, 0.98, 1.00);   // rim frost / sheen

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Nearest-two-cells distance: the seam between them is the crack.
float crackDist(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    float d1 = 8.0, d2 = 8.0;
    for (int y = -1; y <= 1; y++)
    for (int x = -1; x <= 1; x++) {
        vec2 g = vec2(float(x), float(y));
        vec2 o = g + vec2(hash(i + g), hash(i + g + 19.7)) - f;
        float d = dot(o, o);
        if (d < d1) { d2 = d1; d1 = d; } else if (d < d2) { d2 = d; }
    }
    return sqrt(d2) - sqrt(d1);
}

void main() {
    vec2 q = floor(v_texCoords * CELLS) / CELLS;   // snap to the pixel grid
    vec2 p = (q - 0.5) * 2.0;                      // -1..1, quad is already floor-foreshortened

    // Slab: an ellipse filling the quad, edge pushed in and out by noise.
    float ang    = atan(p.y, p.x);
    float wobble = hash(floor(vec2(ang * EDGE_SCALE, 1.0))) - 0.5;
    float r      = length(p) + wobble * EDGE_RAGGED;
    if (r > 0.95) discard;

    // Cracks, and a frosted lip along the rim.
    float crack  = 1.0 - step(CRACK_WIDTH, crackDist(q * CRACK_SCALE));
    float rimZ   = step(0.80, r);

    // Flat facets: pick a tone per crack cell so each pane has its own shade.
    float facet  = hash(floor(q * CRACK_SCALE) + 3.1);
    vec3  tone   = facet < 0.35 ? TONE1 : (facet < 0.75 ? TONE2 : TONE1 * 0.92);
    tone = mix(tone, TONE3, rimZ);
    tone = mix(tone, TONE0, crack * CRACK_LEVEL);

    // Glassy highlight sweeping diagonally across the sheet.
    float sweep = fract((q.x + q.y) * 0.5 - u_time * SHEEN_RATE);
    float sheen = step(1.0 - SHEEN_WIDTH, sweep) * (1.0 - crack);
    tone += TONE3 * sheen * SHEEN_LEVEL;

    vec4 texel = texture2D(u_texture, v_texCoords);  // white pixel — keeps the sampler live
    float a = COVER * u_alpha;
    gl_FragColor = vec4(tone * u_tint * a, a) * texel;
}
