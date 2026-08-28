// Shield: a translucent blue dome held in front of the caster — a sphere
// centered on the body, sliced off at the feet, with an elliptical base where
// it meets the floor. Solid at the leading edge, fading to nothing toward the
// back, with a hairline rim where the surface turns edge-on.
// Blend: GL_ONE, GL_ONE_MINUS_SRC_ALPHA (premultiplied) — rgb adds the blue
// glow, alpha tints what's behind it just enough to read as a surface.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_texture;  // 1x1 white pixel; sampled to keep SpriteBatch happy
uniform float u_time;         // seconds since cast
uniform float u_dir;          // +1 caster faces right (player), -1 faces left (enemy)
uniform vec3  u_tint;         // skill tint; white = the blues below
uniform float u_alpha;        // phase envelope 0..1 (expand in, fade out)

// ---- tuning ----
const float RADIUS      = 0.80;   // dome radius; the quad spans -1..1
const float EDGE_SOFT   = 0.03;   // softness of the dome's outer edge
// Where the feet are in quad space. ShieldVisual centers the quad half a panel
// height above the feet and sizes it 1.6x the larger panel dimension, so the
// feet land at -0.5. Move this if that math changes.
const float GROUND_Y    = -0.50;
const float GROUND_SOFT = 0.02;   // softness of the cut at the feet
const float BASE_SQUASH = 0.28;   // ground ellipse height/width — the floor seen from the camera's angle
const float BASE_WIDTH  = 0.012;  // base rim half-thickness
const float BASE_LEVEL  = 0.9;    // base rim brightness
const float BACK_FADE   = 0.75;   // where along the sphere (back=-1 .. front=+1) alpha starts; lower = more of the back shows
const float FRONT_FULL  = 0.80;   // where it reaches full strength
const float FILL_LEVEL  = 0.35;   // body translucency: how much blue the surface adds at the front
const float OCCLUSION   = 0.30;   // how much the surface dims/tints the sprite behind it
const float RIM_WIDTH   = 0.012;  // bright rim half-thickness at the leading edge — hairline
const float RIM_LEVEL   = 1.0;    // rim brightness; >1 blooms
const float SHINE_LEVEL = 0.25;   // soft highlight near the rim, sphere shading
const float PULSE_RATE  = 2.2;    // gentle breathing, cycles per second
const float PULSE_DEPTH = 0.12;   // 0 = steady
const vec3  FILL_COLOR  = vec3(0.25, 0.55, 1.00);  // body blue
const vec3  RIM_COLOR   = vec3(0.70, 0.90, 1.00);  // pale electric edge

void main() {
    vec2 p = (v_texCoords - 0.5) * 2.0;   // centered
    p.x *= u_dir;                          // flight space: +x is always "ahead"
    p.y  = -p.y;                           // texture V runs top-down; make +y up so the feet are below

    // Front-to-back gradient along the sphere: solid ahead, gone behind.
    float along = p.x / RADIUS;
    float grad  = smoothstep(-BACK_FADE, FRONT_FULL, along);
    float pulse = 1.0 - PULSE_DEPTH * 0.5 * (1.0 - cos(u_time * PULSE_RATE * 6.28318));

    // The base: where the sphere meets the floor — an ellipse as wide as the
    // sphere's cross-section at foot height, squashed by the camera angle.
    // Only its front arc shows; the back arc is behind the caster.
    float halfW  = sqrt(max(RADIUS * RADIUS - GROUND_Y * GROUND_Y, 1e-4));
    vec2  q      = vec2(p.x / halfW, (p.y - GROUND_Y) / (halfW * BASE_SQUASH));
    float bd     = (length(q) - 1.0) * halfW;                      // ~distance to the ellipse
    float inside = 1.0 - smoothstep(-GROUND_SOFT, GROUND_SOFT, bd);
    float front  = 1.0 - smoothstep(GROUND_Y, GROUND_Y + GROUND_SOFT, p.y);
    float base   = (1.0 - smoothstep(0.0, BASE_WIDTH, abs(bd))) * front;
    float bgrad  = smoothstep(-BACK_FADE, FRONT_FULL, p.x / halfW);

    // The dome: the sphere down to the base's front arc — above the ground
    // line, or below it but still inside the ellipse (the dome's lower lip).
    float r     = length(p);
    float dome  = 1.0 - smoothstep(RADIUS - EDGE_SOFT, RADIUS + EDGE_SOFT, r);
    float above = max(smoothstep(GROUND_Y - GROUND_SOFT, GROUND_Y + GROUND_SOFT, p.y), inside);
    float edge  = smoothstep(RADIUS * 0.55, RADIUS, r);
    float shine = edge * SHINE_LEVEL;
    float rim   = (1.0 - smoothstep(0.0, RIM_WIDTH, abs(r - RADIUS))) * smoothstep(-0.2, 0.5, along);
    float body  = dome * above * grad * pulse;

    vec3  light = (FILL_COLOR * (FILL_LEVEL + shine) * body
                 + RIM_COLOR  * rim  * RIM_LEVEL * above
                 + RIM_COLOR  * base * BASE_LEVEL * bgrad) * u_tint;
    float occ   = OCCLUSION * body;

    vec4 texel = texture2D(u_texture, v_texCoords);  // white pixel — keeps the sampler live
    gl_FragColor = vec4(light * u_alpha, occ * u_alpha) * texel;
}
