package com.silverignis.traits;

import com.badlogic.gdx.graphics.Texture;



public final class Trait {

    private final String id;
    private final String displayName;
    private final String description;
    private final Texture icon;
    private final TraitEffect effect;

    public Trait(String id, String displayName, String description, Texture icon, TraitEffect effect) {
        this.id = require(id,"id");
        this.displayName = require(displayName, "displayName");
        this.description = require(description, "description");
        this.icon = require(icon, "icon");
        this.effect = require(effect, "effect");
    }

    private <T> T require(T value, String field) {
        if (value == null) throw new IllegalStateException("Trait '" + id + "' missing required field '" + field + "'");
        return value;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Texture getIcon() { return icon; }
    public TraitEffect getEffect() { return effect; }
}
