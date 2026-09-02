attribute vec3 a_position;      // tile-local
attribute vec4 a_color;
attribute float a_edge;         // -1 .. +1 across the ribbon

uniform mat4 u_projTrans;
uniform vec2 u_tileCenter;      // world x/z of the tile

varying vec4 v_color;
varying float v_edge;

void main() {
    v_color = a_color;
    v_edge = a_edge;
    gl_Position = u_projTrans * vec4(a_position.x + u_tileCenter.x, a_position.y,
                                     a_position.z + u_tileCenter.y, 1.0);
}
