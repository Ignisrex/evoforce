attribute vec2 a_position;      // unit grid, [-0.5, 0.5]^2

uniform mat4 u_projTrans;
uniform vec4 u_tile;            // centerX, centerZ, halfW, halfD

varying vec2 v_uv;
varying vec3 v_world;

void main() {
    v_uv = a_position + 0.5;
    vec2 world = vec2(u_tile.x + a_position.x * 2.0 * u_tile.z,
                      u_tile.y + a_position.y * 2.0 * u_tile.w);
    v_world = vec3(world.x, 0.032, world.y);
    gl_Position = u_projTrans * vec4(v_world, 1.0);
}
