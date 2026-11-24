package com.silverignis.input;

public record InputEvent(GameAction action, Type type){
    public enum Type { PRESSED, RELEASED }
}
