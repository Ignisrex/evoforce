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

// jagged bolt across the tile: glow falls off with distance to a noise-displaced path
float arc(vec2 uv, float seed, float t) {
    float y = 0.5 + 0.70 * (vnoise(vec2(uv.x * 3.0 + seed * 17.0, t)) - 0.5)
                  + 0.16 * (vnoise(vec2(uv.x * 12.0 + seed * 31.0, t * 1.7)) - 0.5);
    float on = step(0.35, hash(vec2(t, seed)));          // arcs blink on/off per jump
    return on * exp(-abs(uv.y - y) * 55.0);
}

void main() {
    float t = floor(u_time * 8.0);                       // bolt paths jump 8x/sec

    // dark storm base with a faint drifting plasma haze
    vec3 col = mix(vec3(0.05, 0.05, 0.08), u_side, 0.45);
    float haze = fbm(v_uv * 3.0 + vec2(u_time * 0.10, -u_time * 0.06));
    col += vec3(0.10, 0.10, 0.04) * haze;

    // two horizontal bolts + one vertical (axes swapped)
    float a = arc(v_uv, 1.0, t) + arc(v_uv, 2.0, t + 37.0) + arc(v_uv.yx, 3.0, t + 71.0);
    col += vec3(1.00, 0.95, 0.55) * min(a, 1.4);

    gl_FragColor = vec4(col, 1.0);
}
