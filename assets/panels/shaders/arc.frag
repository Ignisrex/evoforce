#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying float v_edge;

void main() {
    // bright core fading softly to the ribbon edges; drawn additive, bloom does the rest
    float a = 1.0 - abs(v_edge);
    a *= a;
    gl_FragColor = vec4(v_color.rgb * a, a);
}
