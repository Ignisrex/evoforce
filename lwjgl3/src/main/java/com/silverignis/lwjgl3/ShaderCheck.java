package com.silverignis.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/** Throwaway: compiles every ui shader against a real GL context and reports. */
public final class ShaderCheck extends ApplicationAdapter {
    private static final String[][] PAIRS = {
        {"ui/shaders/sprite_batch.vert", "ui/shaders/celestial_veil.frag"},
        {"ui/shaders/sprite_batch.vert", "ui/shaders/menu_backdrop.frag"},
        {"ui/shaders/rounded_rect.vert", "ui/shaders/aurora.frag"},
        {"ui/shaders/rounded_rect.vert", "ui/shaders/rounded_rect.frag"},
        {"ui/shaders/sprite_batch.vert", "skills/shaders/dark_blast.frag"},
        {"ui/shaders/sprite_batch.vert", "skills/shaders/wind_slash.frag"},
        {"ui/shaders/sprite_batch.vert", "skills/shaders/shield.frag"},
        {"ui/shaders/sprite_batch.vert", "skills/shaders/frost_trap.frag"},
        {"panels/shaders/fire.vert", "panels/shaders/fire.frag"},
        {"panels/shaders/ice.vert", "panels/shaders/ice.frag"},
        {"panels/shaders/lightning.vert", "panels/shaders/lightning.frag"},
        {"panels/shaders/dark.vert", "panels/shaders/dark.frag"},
        {"panels/shaders/poison.vert", "panels/shaders/poison.frag"},
        {"panels/shaders/nature.vert", "panels/shaders/nature.frag"},
        {"panels/shaders/grass.vert", "panels/shaders/grass.frag"},
        {"panels/shaders/arc.vert", "panels/shaders/arc.frag"},
        {"panels/shaders/flame.vert", "panels/shaders/flame.frag"},
    };

    @Override
    public void create() {
        int bad = 0;
        for (String[] pair : PAIRS) {
            ShaderProgram p = new ShaderProgram(
                Gdx.files.internal(pair[0]), Gdx.files.internal(pair[1]));
            if (p.isCompiled()) {
                System.out.println("OK   " + pair[1] + (p.getLog().trim().isEmpty() ? "" : "  log: " + p.getLog().trim()));
            } else {
                bad++;
                System.out.println("FAIL " + pair[1] + "\n" + p.getLog());
            }
            p.dispose();
        }
        System.out.println("shader-check failures: " + bad);
        Gdx.app.exit();
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setWindowedMode(320, 200);
        c.setTitle("shader-check");
        new Lwjgl3Application(new ShaderCheck(), c);
    }
}
