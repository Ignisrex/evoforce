#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;
uniform float u_time;
uniform vec4 u_tile;            // per-tile seed for twinkle placement
uniform vec3 u_side;            // team color under the phenomenon
uniform sampler2D u_star;       // effect star sprite — the twinkle itself

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

    // frosted, milky sheet — high-key so it reads as ice, not water
    float n = fbm(p * 2.2);
    vec3 frost = vec3(0.80, 0.88, 0.94);
    vec3 shade = mix(vec3(0.58, 0.72, 0.84), u_side, 0.30);
    vec3 col = mix(shade, frost, n);

    // crack lines, slightly darker glacial blue (static — ice doesn't move)
    float c = abs(vnoise(p * 3.2 + 11.0) - 0.5) * 2.0;
    col = mix(vec3(0.40, 0.58, 0.75), col, smoothstep(0.0, 0.08, c));

    // fine frost grain
    col += (vnoise(p * 18.0) - 0.5) * 0.05;

    // star-sprite twinkles: a few spots per tile, each pulsing on its own phase
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        vec2 pos = vec2(hash(u_tile.xy + fi), hash(u_tile.yx + fi + 9.0)) * 0.6 + 0.2;
        float size = 0.16 + 0.10 * hash(vec2(fi, 3.7));
        vec2 local = (v_uv - pos) / size + 0.5;
        if (local.x > 0.0 && local.x < 1.0 && local.y > 0.0 && local.y < 1.0) {
            vec4 s = texture2D(u_star, local);
            float speed = 1.2 + 1.1 * hash(vec2(fi, 8.2));
            float tw = 0.15 + 0.85 * pow(0.5 + 0.5 * sin(u_time * speed + hash(vec2(fi, 1.3)) * 6.2832 + u_tile.x), 3.0);
            col += vec3(0.95, 0.98, 1.00) * s.a * dot(s.rgb, vec3(0.299, 0.587, 0.114)) * tw;
        }
    }

    gl_FragColor = vec4(col, 1.0);
}
