#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;
varying vec3 v_world;
uniform float u_time;
uniform vec3 u_camPos;
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
    // view ray through this fragment, continued below the floor: parallax layers
    // at fake depths make the panel read as a pit instead of a painted square.
    vec3 dir = normalize(v_world - u_camPos);
    float dy = max(0.12, -dir.y);                        // guard near-grazing rays

    vec3 col = mix(vec3(0.015, 0.008, 0.045), u_side * 0.25, 0.50);   // abyss base, side-hued

    // three wisp layers, deeper = dimmer and slower
    for (int i = 0; i < 3; i++) {
        float depth = 0.25 + float(i) * 0.45;
        vec2 at = v_world.xz + dir.xz * (depth / dy);
        float n = fbm(at * 2.2 + vec2(u_time * 0.05, -u_time * 0.03) * (1.0 - 0.25 * float(i)));
        float wisp = smoothstep(0.55, 0.85, n);
        col += vec3(0.30, 0.10, 0.52) * wisp * (0.55 - 0.16 * float(i));
    }

    // sparse motes drifting on the deepest layer
    vec2 deep = v_world.xz + dir.xz * (1.4 / dy);
    float mote = step(0.995, hash(floor(deep * 14.0) + floor(u_time * 0.5)));
    col += vec3(0.55, 0.35, 0.85) * mote;

    gl_FragColor = vec4(col, 1.0);
}
