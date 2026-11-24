package com.silverignis.screens.state;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.silverignis.input.InputFrame;

public interface GameScreenState {

    void onEnter();

    void onExit();

    void input(InputFrame input);

    void update(float delta);

    void render(SpriteBatch batch);

    default void resize(int width, int height) {}
}
