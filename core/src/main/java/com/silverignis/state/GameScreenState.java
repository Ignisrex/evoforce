package com.silverignis.state;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public interface GameScreenState {

    void onEnter();

    void onExit();

    void input();

    void update(float delta);

    void render(SpriteBatch batch);

    default void resize(int width, int height) {}
}
