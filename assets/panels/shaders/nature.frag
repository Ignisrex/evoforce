#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;
uniform vec3 u_side;

void main() {
    // plain grass-green ground under the blades
    vec3 col = mix(vec3(0.11, 0.28, 0.09), u_side, 0.25);
    gl_FragColor = vec4(col, 1.0);
}
