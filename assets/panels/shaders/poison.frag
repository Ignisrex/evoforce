#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;
uniform float u_time;
uniform vec3 u_side;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i),                  hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++) { v += a * vnoise(p); p *= 2.03; a *= 0.5; }
    return v;
}

void main() {
    vec2 p = v_uv * vec2(2.6, 1.5);

    // slow churning sludge
    float n = fbm(p + 0.8 * fbm(p + vec2(u_time * 0.06, -u_time * 0.04)));
    vec3 murk   = mix(vec3(0.04, 0.09, 0.02), u_side, 0.40);
    vec3 sludge = vec3(0.14, 0.42, 0.08);
    vec3 toxin  = vec3(0.30, 0.14, 0.36);                // purple toxicity patches
    vec3 col = mix(murk, sludge, smoothstep(0.35, 0.75, n));
    col = mix(col, toxin, 0.5 * smoothstep(0.75, 0.95, fbm(p * 1.7 - u_time * 0.03)));

    // rising bubbles: cells pulse up to a bright rim, then reset
    vec2 bp = p * 5.0;
    vec2 cell = floor(bp);
    float phase = fract(u_time * (0.25 + 0.35 * hash(cell)) + hash(cell + 7.0));
    float r = length(fract(bp) - 0.5);
    float ring = smoothstep(0.05, 0.0, abs(r - 0.32 * phase)) * (1.0 - phase);
    col += vec3(0.45, 0.95, 0.30) * ring * step(0.55, hash(cell + 3.0));

    gl_FragColor = vec4(col, 1.0);
}
