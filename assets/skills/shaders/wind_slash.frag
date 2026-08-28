// Wind slash: long straight white wind lines trailing behind the crescent,
// spanning its full height — per the concept sketch: varied start points near
// the slash's back, long tails streaming behind it.
// Blend: GL_ONE, GL_ONE_MINUS_SRC_ALPHA (premultiplied) — pure added light,
// alpha stays 0 so nothing behind the quad is darkened.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_texture;  // 1x1 white pixel; sampled to keep SpriteBatch happy
uniform float u_time;         // seconds since the projectile spawned
uniform float u_dir;          // +1 flying right (player shot), -1 flying left (enemy)
uniform vec3  u_tint;         // skill tint; white = plain white lines

// The quad is 2 sprite-widths wide and exactly sprite-height tall, so
// p.y = -1..1 IS the crescent's height and the sprite spans p.x -0.5..0.5.

// ---- tuning ----
const float LINE_COUNT = 3.0;    // lanes, evenly spaced
const float SPREAD     = 0.65;   // vertical reach of the lanes, fraction of the crescent's height
const float JITTER_Y   = 0.10;   // small random offset off a lane's even spacing
const float HALF_WIDTH = 0.007;  // line half-thickness, fraction of sprite half-height — a pixel
const float EDGE_SOFT  = 0.009;  // line edge softness
const float DASH_LEN   = 0.28;   // longest a line can be
const float LEN_VARY   = 0.55;   // per-lane length variation: 1-LEN_VARY .. 1 of DASH_LEN
const float START_X    = 0.15;   // furthest forward a line may begin (near the crescent's back)
const float START_VARY = 0.45;   // how much earlier/later each lane starts, hashed
const float SPEED      = 1.6;    // slide speed, quad-units per second
const float EDGE_FADE  = 0.80;   // lines die out before the back edge of the quad
const float BOOST      = 0.6;    // brightness; <1 reads translucent (additive light)
const vec3  WIND_COLOR = vec3(0.96, 0.96, 0.96);  // plain white, no tint

// ---- helpers ----

// 1D -> 1D hash in 0..1, mediump-safe.
float hash11(float n) {
    vec2 p = fract(vec2(n * 0.1031, n * 0.1030) + 0.37);
    p += dot(p, p.yx + 33.33);
    return fract(p.x * p.y);
}

// One long line sliding backward on its lane, born near the crescent's back.
float streak(vec2 p, float i, float time) {
    float y0   = ((i + 0.5) / LINE_COUNT - 0.5) * 2.0 * SPREAD;
    float y    = y0 + (hash11(i * 3.1 + 0.7) - 0.5) * 2.0 * JITTER_Y;
    float line = 1.0 - smoothstep(HALF_WIDTH, HALF_WIDTH + EDGE_SOFT, abs(p.y - y));

    // Each lane's head is born at its own point near the slash, runs backward,
    // wraps. Each lane also rolls its own line length.
    float len   = DASH_LEN * (1.0 - LEN_VARY * hash11(i * 9.1 + 2.6));
    float born  = START_X - hash11(i * 5.3 + 1.9) * START_VARY;
    float cycle = (born + 1.0 + len) / SPEED;
    float head  = born - mod(time + hash11(i * 7.7 + 4.2) * cycle, cycle) * SPEED;
    float s     = (p.x - head) / len;                            // 0 at head, 1 at tail end
    float dash  = smoothstep(0.0, 0.10, s) * smoothstep(1.0, 0.90, s);
    // Nothing shows ahead of where this lane is born.
    float back  = 1.0 - smoothstep(born, born + 0.06, p.x);

    return line * dash * back;
}

void main() {
    vec2 p = (v_texCoords - 0.5) * 2.0;   // centered
    p.x *= u_dir;                          // flight space: +x is always "ahead"

    float lines = 0.0;
    for (float i = 0.0; i < LINE_COUNT; i += 1.0) {
        lines += streak(p, i, u_time);
    }

    // Fade out before the quad's back edge so nothing clips.
    float inside = 1.0 - smoothstep(EDGE_FADE, 0.98, -p.x);

    vec3 light = WIND_COLOR * min(lines, 1.0) * inside * BOOST * u_tint;

    vec4 texel = texture2D(u_texture, v_texCoords);  // white pixel — keeps the sampler live
    gl_FragColor = vec4(light, 0.0) * texel;         // alpha 0: adds light, occludes nothing
}
