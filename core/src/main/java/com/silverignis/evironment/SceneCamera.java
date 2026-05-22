package com.silverignis.evironment;

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
    public Vector2 project(float worldX, float worldZ) {
        tmp.set(worldX, 0f, worldZ);
        cam.project(tmp);
        float wx = (tmp.x - viewport.getScreenX()) / viewport.getScreenWidth()  * viewport.getWorldWidth();
        float wy = (tmp.y - viewport.getScreenY()) / viewport.getScreenHeight() * viewport.getWorldHeight();
        return new Vector2(wx, wy);
    }

    /** Perspective sprite scale at floor depth z: 1.0 near, smaller toward the back. */
    public float depthScale(float worldZ) {
        float ny = (NEAR_FLOOR_Z - worldZ) / FLOOR_DEPTH;  // 0 near, 1 far
        return 1.0f - ny * DEPTH_SCALE_FAR;
    }
}
