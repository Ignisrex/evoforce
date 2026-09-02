#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;      // u -1..+1 across the card, v 0..1 base to tip
varying vec2 v_fx;      // phase, heat
uniform float u_time;

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
    float u = v_uv.x, v = v_uv.y;

    // teardrop silhouette: allowed half-width narrows toward the tip
    float profile = pow(1.0 - v, 0.55);
    float shape = 1.0 - smoothstep(profile * 0.7, profile, abs(u));

    // upward-scrolling erosion — licks form and pinch off at the top
    float n = fbm(vec2(u * 2.2 + v_fx.x, v * 3.0 - u_time * 2.6 + v_fx.x * 4.0));
    float fire = shape * (1.0 - v * 0.85) * (0.5 + 0.8 * n);

    float body = smoothstep(0.16, 0.34, fire);
    float core = smoothstep(0.40, 0.68, fire);

    // hottest at the base core: yellow-white -> orange -> deep red at the fringes
    vec3 col = mix(vec3(0.60, 0.07, 0.01), vec3(1.00, 0.42, 0.05), body);
    col = mix(col, vec3(1.00, 0.90, 0.50), core * (1.0 - v * 0.5));

    float heat = 0.85 + 0.15 * v_fx.y;
    gl_FragColor = vec4(col * body * heat, body);
}
