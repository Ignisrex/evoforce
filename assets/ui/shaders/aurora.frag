// Aurora night sky for the reward screen: deep space gradient, a starfield,
// slow nebula haze, and two aurora beams — teal and violet — that weave
// across the same band of sky. Each is a sharp-cored band around a
// domain-warped spine, its fringe leaning toward the other beam's colour,
// with soft vertical folds drifting along it. Smooth gradients only — no
// quantised bands. Everything is procedural (hash + value noise); the only
// texture is the 1x1 pixel the SpriteBatch wants. Colours are uniforms so the
// palette can be tuned per screen without touching GLSL.
#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture; // 1x1 white pixel; sampled to keep SpriteBatch happy
uniform float u_time;
uniform float u_aspect;      // width / height, so stars and waves stay round
uniform vec3 u_skyTop;
uniform vec3 u_skyBottom;
uniform vec3 u_auroraA;      // ribbon body (green-teal in the classic look)
uniform vec3 u_auroraB;      // ribbon fringe / second ribbon (violet-magenta)
uniform vec3 u_nebula;       // faint haze tint behind the aurora
uniform float u_intensity;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// rotate between octaves so the lattice never lines up (kills grid-y streaks)
const mat2 ROT = mat2(0.80, 0.60, -0.60, 0.80);

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise(p);
        p = ROT * p * 2.03 + vec2(17.0, 9.0);
        amp *= 0.5;
    }
    return v;
}

// One ribbon in its own colour. p is aspect-corrected sky space; the ribbon's
// frame is domain-warped by slow fbm so it bends and swells instead of
// following a pure sine. Section is a super-gaussian in signed distance d
// (−1 below the spine .. +1 above): a bright plateau with a firm, smooth
// edge and a faint halo — reads as a solid ribbon, never bands. The fringe
// leans toward the other beam's colour so the two knit where they cross.
vec3 ribbon(vec2 p, float baseY, float phase, float speed, float thickness,
            vec3 colour, vec3 other) {
    float t = u_time * speed;
    vec2 q = vec2(fbm(p * 1.3 + vec2(t * 0.15, phase)),
                  fbm(p * 1.3 + vec2(5.2, 1.3 + phase + t * 0.10)));
    vec2 w = p + 0.18 * (q - 0.5);

    float spine = baseY
        + 0.13 * sin(w.x * 1.6 + t + phase)
        + 0.05 * sin(w.x * 3.9 - t * 1.3 + phase * 2.0)
        + 0.08 * (fbm(vec2(w.x * 1.1 + t * 0.12, phase)) - 0.5);
    float d = (w.y - spine) / thickness;

    // curtain folds: vertical streaks that drift along the warped x
    float folds = fbm(vec2(w.x * 7.0 + t * 0.3, phase * 3.0 + w.y * 0.5));
    folds = 0.3 + 0.7 * smoothstep(0.3, 0.8, folds);

    // super-gaussian: bright plateau across most of the thickness, then a
    // firm-but-smooth edge, with a faint halo beyond it
    float d2 = d * d;
    float core = exp(-d2 * d2 * 3.0);
    float halo = exp(-d2 * 1.5);
    float body = 0.8 * core + 0.2 * halo;

    vec3 col = mix(colour, other, 0.4 * smoothstep(0.3, 1.2, abs(d)));
    col = mix(col, vec3(1.0), 0.3 * exp(-d * d * 30.0));   // white-hot centre line
    return col * body * folds;
}

float stars(vec2 p) {
    vec2 g = p * 90.0;
    vec2 cell = floor(g);
    vec2 f = fract(g) - 0.5;
    float h = hash(cell);
    if (h < 0.92) return 0.0;
    vec2 off = vec2(hash(cell + 1.7), hash(cell + 3.1)) - 0.5;
    float d = length(f - off * 0.6);
    float twinkle = 0.6 + 0.4 * sin(u_time * (1.5 + h * 3.0) + h * 40.0);
    float size = mix(0.06, 0.16, hash(cell + 5.3));
    return smoothstep(size, 0.0, d) * twinkle * (h - 0.92) / 0.08;
}

void main() {
    // SpriteBatch quads have v = 0 at the top; flip so y grows upward
    vec2 uv = vec2(v_texCoords.x, 1.0 - v_texCoords.y);
    vec2 p = vec2(uv.x * u_aspect, uv.y);

    vec3 col = mix(u_skyBottom, u_skyTop, smoothstep(0.0, 1.0, uv.y));

    // nebula haze: fbm(p + fbm(p)) for wispy, cloud-like structure
    vec2 hp = p * 1.4 + vec2(u_time * 0.02, -u_time * 0.01);
    float haze = fbm(hp + 0.6 * (fbm(hp * 0.7 + 3.1) - 0.5));
    haze = smoothstep(0.35, 0.85, haze);
    col += u_nebula * haze * 0.35;

    col += vec3(stars(p + vec2(0.0, u_time * 0.002)));

    // two beams sharing the same band of sky, waves out of phase so they
    // cross and weave; teal one broad, violet one thinner and a touch dimmer
    vec3 aurora = ribbon(p, 0.60, 0.0, 0.32, 0.22, u_auroraA, u_auroraB)
                + ribbon(p, 0.64, 2.7, 0.26, 0.16, u_auroraB, u_auroraA) * 0.8;
    col += aurora * u_intensity;

    float v = smoothstep(1.4, 0.4, length(uv - 0.5));
    col *= mix(0.7, 1.0, v);

    // soft-clip the additive stack (ACES fit) so bright overlaps roll off
    // toward white instead of flattening into a solid cyan
    col = clamp((col * (2.51 * col + 0.03)) / (col * (2.43 * col + 0.59) + 0.14), 0.0, 1.0);

    gl_FragColor = vec4(col, 1.0) * v_color * texture2D(u_texture, v_texCoords);
}
