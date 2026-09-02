attribute vec3 a_base;          // tile-local card base (on the lava)
attribute vec2 a_corner;        // u -1..+1 across, v 0..1 up
attribute vec2 a_size;          // halfWidth, height
attribute vec2 a_fx;            // per-flame phase, heat

uniform mat4 u_projTrans;
uniform vec2 u_tileCenter;      // world x/z of the tile
uniform vec2 u_camRight;        // horizontal camera-right (x, z), normalized
uniform float u_time;

varying vec2 v_uv;
varying vec2 v_fx;

void main() {
    v_uv = a_corner;
    v_fx = a_fx;

    // cylindrical billboard: expand around the vertical axis toward the camera
    float sway = sin(u_time * 2.1 + a_fx.x) * 0.030 * a_corner.y;
    vec3 p = a_base;
    p.x += u_camRight.x * a_corner.x * a_size.x + sway;
    p.z += u_camRight.y * a_corner.x * a_size.x + sway * 0.6;
    p.y += a_corner.y * a_size.y;

    gl_Position = u_projTrans * vec4(p.x + u_tileCenter.x, p.y, p.z + u_tileCenter.y, 1.0);
}
