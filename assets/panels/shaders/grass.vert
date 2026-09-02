attribute vec3 a_position;      // tile-local, y up from slab top
attribute vec4 a_color;
attribute vec2 a_sway;          // per-blade phase, height weight (0 base .. 1 tip)

uniform mat4 u_projTrans;
uniform vec2 u_tileCenter;      // world x/z of the tile
uniform float u_time;

varying vec4 v_color;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i),                  hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    v_color = a_color;
    vec3 p = a_position;
    vec2 wxz = vec2(p.x + u_tileCenter.x, p.z + u_tileCenter.y);

    // shared gust field scrolling across the world — neighboring blades sway together,
    // with a small per-blade flutter on top
    float gust = vnoise(wxz * 0.8 - vec2(u_time * 0.55, u_time * 0.20)) * 2.0 - 1.0;

    float w = a_sway.y;
    p.x += (gust * 0.07  + sin(u_time * 1.6 + a_sway.x) * 0.02)  * w;
    p.z += (gust * 0.035 + sin(u_time * 1.1 + a_sway.x * 1.7) * 0.015) * w;

    gl_Position = u_projTrans * vec4(p.x + u_tileCenter.x, p.y, p.z + u_tileCenter.y, 1.0);
}
