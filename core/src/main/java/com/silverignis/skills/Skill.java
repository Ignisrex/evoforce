package com.silverignis.skills;

import com.badlogic.gdx.graphics.Texture;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.visuals.SkillVisuals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Gameplay definition of a skill. Its look lives in a {@code SkillVisual}
 *  registered under the same id in {@link SkillVisuals}; nothing here names a
 *  texture, tint or effect beyond the menu icon. */
public final class Skill {

    public enum Shape{
        PROJECTILE,
        BEAM,
        STRIKE,
        AURA,
        ZONE
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final Texture icon;

    //aspects of a skill
    private final Shape shape;
    private final Element element;
    private final List<Effect> effects;

    private final float cooldown;

    private final ShapeConfig shapeConfig;

    private final float powerScale;
    private final float magicScale;
    private final int manaCost;

    private Skill(Builder b) {
        this.id = b.id;
        this.displayName = b.displayName;
        this.description = b.description;
        this.icon = b.icon;
        this.shape = b.shape;
        this.element = b.element;
        this.effects = Collections.unmodifiableList(new ArrayList<>(b.effects));
        this.cooldown = b.cooldown;
        this.shapeConfig = b.shapeConfig;
        this.powerScale = b.powerScale;
        this.magicScale = b.magicScale;
        this.manaCost = b.manaCost;
    }

    public static Builder builder() { return new Builder(); }

    public String       getId()          { return id; }
    public String       getDisplayName() { return displayName; }
    public String       getDescription() { return description; }
    public Texture      getIcon()        { return icon; }
    public Shape        getShape()       { return shape; }
    public Element      getElement()     { return element; }
    public List<Effect> getEffects()     { return effects; }
    public float        getCooldown()    { return cooldown; }
    public ShapeConfig  getShapeConfig() { return shapeConfig; }
    public float        getPowerScale() {return powerScale; }
    public float        getMagicScale() { return magicScale; }
    public int          getManaCost()   { return manaCost; }


    public static final class Builder {
        private String id;
        private String displayName;
        private String description;
        private Texture icon;
        private Shape shape;
        private Element element;
        private final List<Effect> effects = new ArrayList<>();
        private float cooldown;
        private boolean cooldownSet;
        private ShapeConfig shapeConfig;
        private float powerScale = 0f;
        private float magicScale = 0f;
        private int manaCost = 1;

        private Builder() {}

        public Builder id(String v)                                 { this.id = v; return this; }
        public Builder displayName(String v)                        { this.displayName = v; return this; }
        public Builder description(String v)                        { this.description = v; return this; }
        public Builder icon(Texture v)                              { this.icon = v; return this; }
        public Builder shape(Shape v)                               { this.shape = v; return this; }
        public Builder element(Element v)                           { this.element = v; return this; }
        public Builder cooldown(float v)                            { this.cooldown = v; this.cooldownSet = true; return this; }
        public Builder shapeConfig(ShapeConfig v)                   { this.shapeConfig = v; return this; }
        public Builder powerScale(float v)                          { this.powerScale = v; return this; }
        public Builder magicScale(float v)                          { this.magicScale = v; return this; }
        public Builder manaCost(int v)                              { this.manaCost = v; return this; }

        public Builder effect(Effect e)                             { this.effects.add(e); return this; }
        public Builder effects(List<Effect> e) {
            this.effects.clear();
            if (e != null) this.effects.addAll(e);
            return this;
        }

        public Skill build() {
            require(id,          "id");
            // Fails at load, not on first cast: every skill must have a look.
            if (!SkillVisuals.has(id)) throw new IllegalStateException(
                "Skill '" + id + "' has no visual registered in SkillVisuals");
            require(displayName, "displayName");
            require(description, "description");
            require(icon,        "icon");
            require(shape,       "shape");
            require(element,     "element");
            if (!cooldownSet) throw missing("cooldown");
            return new Skill(this);
        }

        private void require(Object v, String field) {
            if (v == null) throw missing(field);
        }

        private IllegalStateException missing(String field) {
            String which = id != null ? id : "<unknown>";
            return new IllegalStateException(
                "Skill '" + which + "' missing required field '" + field + "'");
        }
    }
}
