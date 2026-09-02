attribute vec2 a_position;      // unit grid, [-0.5, 0.5]^2

uniform mat4 u_projTrans;
uniform vec4 u_tile;            // centerX, centerZ, halfW, halfD
uniform float u_time;

varying vec2 v_uv;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i),                  hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    v_uv = a_position + 0.5;
    vec2 world = vec2(u_tile.x + a_position.x * 2.0 * u_tile.z,
                      u_tile.y + a_position.y * 2.0 * u_tile.w);
    float h = 0.012 * vnoise(world * 2.5 + vec2(u_time * 0.4, u_time * 0.25));
    gl_Position = u_projTrans * vec4(world.x, 0.033 + h, world.y, 1.0);
}
