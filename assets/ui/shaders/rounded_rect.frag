// Rounded rect via signed distance: d < 0 inside the shape, with the border
// ring occupying the last u_borderWidth units before the edge, and a neon
// glow falling off quadratically for u_glowWidth units past it (the quad is
// inflated by that much on the CPU side so the spill has room). fwidth()
// gives one screen-pixel of anti-aliasing regardless of world or Group scale.
#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;  // 1x1 white pixel; sampled to keep SpriteBatch happy
uniform vec2 u_halfSize;      // rect half-extents, local (world) units
uniform float u_radius;       // corner radius, same units
uniform float u_borderWidth;  // border ring thickness, same units
uniform float u_glowWidth;    // glow reach past the edge, same units (0 = none)
uniform vec4 u_fillColor;
uniform vec4 u_borderColor;
uniform vec4 u_glowColor;

float roundBoxDist(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 quadHalf = u_halfSize + vec2(u_glowWidth);
    vec2 p = (v_texCoords - 0.5) * quadHalf * 2.0;
    float r = min(u_radius, min(u_halfSize.x, u_halfSize.y));
    float d = roundBoxDist(p, u_halfSize, r);
    float aa = fwidth(d);

    float shape = 1.0 - smoothstep(-aa, 0.0, d);
    float fillArea = 1.0 - smoothstep(-u_borderWidth - aa, -u_borderWidth, d);
    vec4 body = mix(u_borderColor, u_fillColor, fillArea);

    float glowFall = 1.0 - clamp(d / max(u_glowWidth, 1e-4), 0.0, 1.0);
    glowFall *= glowFall;

    vec3 rgb = mix(u_glowColor.rgb, body.rgb, shape);
    float alpha = mix(u_glowColor.a * glowFall, body.a, shape);
    gl_FragColor = vec4(rgb, alpha) * v_color * texture2D(u_texture, v_texCoords);
}
