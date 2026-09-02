package com.silverignis.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.silverignis.entities.Battlefield;

/** The common case: one animated frag drawn on the shared tile quad. */
public class ShaderPanelSurface extends PanelSurface {

    protected final ShaderProgram shader;

    public ShaderPanelSurface(Battlefield.PanelType type, String shaderName) {
        super(type);
        this.shader = load(shaderName);
    }

    static ShaderProgram load(String name) {
        ShaderProgram p = new ShaderProgram(
            Gdx.files.internal("panels/shaders/" + name + ".vert"),
            Gdx.files.internal("panels/shaders/" + name + ".frag"));
        if (!p.isCompiled()) throw new IllegalStateException("panel shader " + name + ": " + p.getLog());
        return p;
    }

    @Override public void render(Camera cam) {
        boolean bound = false;
        Battlefield bf = host.battlefield();
        for (int c = 0; c < Battlefield.COLS; c++) {
            for (int r = 0; r < Battlefield.ROWS; r++) {
                if (bf.getPanel(c, r) != type) continue;
                if (!bound) {
                    shader.bind();
                    host.commonUniforms(shader, cam);
                    bound = true;
                }
                host.drawTileQuad(shader, c, r);
            }
        }
    }

    @Override public void dispose() { shader.dispose(); }
}
