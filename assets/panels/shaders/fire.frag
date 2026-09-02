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
    for (int i = 0; i < 4; i++) { v += a * vnoise(p); p *= 2.03; a *= 0.5; }
    return v;
}

void main() {
    vec2 p = v_uv * vec2(3.0, 1.6);
    float n = fbm(p + vec2(u_time * 0.15, u_time * 0.05) + 1.5 * fbm(p - u_time * 0.08));

    vec3 crust = mix(vec3(0.10, 0.02, 0.00), u_side, 0.55);
    vec3 glowc = vec3(1.00, 0.45, 0.08);
    vec3 hot   = vec3(1.00, 0.90, 0.50);
    vec3 col = mix(crust, glowc, smoothstep(0.45, 0.75, n));
    col = mix(col, hot, smoothstep(0.80, 0.95, n));

    gl_FragColor = vec4(col, 1.0);
}
