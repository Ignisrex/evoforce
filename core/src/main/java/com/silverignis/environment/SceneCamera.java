package com.silverignis.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

public class SceneCamera {

    private static final float CAM_X = 0f, CAM_Y = 6.75f, CAM_Z = 7.54f;
    private static final float CAM_FOV = 45f;
    private static final float LOOK_X = 0f, LOOK_Y = 0.33f, LOOK_Z = -0.11f;

    private static final float NEAR_FLOOR_Z   = 2.0f;   // z closest to camera
    private static final float FLOOR_DEPTH     = 5.0f;   // z span front -> back
    private static final float DEPTH_SCALE_FAR = 0.22f;  // far sprites this much smaller

    private final Viewport viewport;
    private final PerspectiveCamera cam;
    private final Vector3 tmp = new Vector3();

    public SceneCamera(Viewport viewport) {
        this.viewport = viewport;
        cam = new PerspectiveCamera(CAM_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(CAM_X, CAM_Y, CAM_Z);
        cam.lookAt(LOOK_X, LOOK_Y, LOOK_Z);
        cam.near = 0.1f;
        cam.far  = 100f;
        cam.update();
    }

    public PerspectiveCamera getCamera() { return cam; }

    public void resize(int w, int h) {
        cam.viewportWidth  = w;
        cam.viewportHeight = h;
        cam.update();
    }

    /** Project a cave-floor point (world X, Z; y=0) to viewport world coords. */
    public Vector2 project(float worldX, float worldZ, Vector2 out) {
        tmp.set(worldX, 0f, worldZ);
        cam.project(tmp);
        float wx = (tmp.x - viewport.getScreenX()) / viewport.getScreenWidth()  * viewport.getWorldWidth();
        float wy = (tmp.y - viewport.getScreenY()) / viewport.getScreenHeight() * viewport.getWorldHeight();
        return out.set(wx, wy);
    }

    public Vector2 project(float worldX, float worldZ) {
        return project(worldX, worldZ, new Vector2());
    }

    /** Perspective sprite scale at floor depth z: 1.0 near, smaller toward the back. */
    public float depthScale(float worldZ) {
        float ny = (NEAR_FLOOR_Z - worldZ) / FLOOR_DEPTH;  // 0 near, 1 far
        return 1.0f - ny * DEPTH_SCALE_FAR;
    }

    private final Vector2 probe0 = new Vector2(), probe1 = new Vector2();

    /** Invert {@link #project} in x at a fixed floor depth: viewport x → world x.
     *  The projection is linear in x for fixed z, so two probe points pin the line. */
    public float unprojectX(float viewportX, float worldZ) {
        project(0f, worldZ, probe0);
        project(1f, worldZ, probe1);
        return (viewportX - probe0.x) / (probe1.x - probe0.x);
    }

    /** Invert the billboard fake-height convention (drawn y = floor y + height·depthScale):
     *  viewport y → world height above the floor at (worldX, worldZ). */
    public float unprojectHeight(float viewportY, float worldX, float worldZ) {
        project(worldX, worldZ, probe0);
        return (viewportY - probe0.y) / depthScale(worldZ);
    }
}
