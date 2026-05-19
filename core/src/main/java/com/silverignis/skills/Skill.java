package com.silverignis.skills;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /** In-battle VFX sprite. Required — every {@link Skill} must supply one. */
    private final Texture vfxTexture;

    /** Optional animated VFX. When present, renderers use this instead of
     *  the static {@code vfxTexture}. */
    private final Animation<TextureRegion> vfxAnimation;

    /** Backing spritesheet for {@link #vfxAnimation}, owned by this skill so
     *  {@link SkillLibrary#dispose()} can release it. {@code null} when there
     *  is no animation. */
    private final Texture vfxAnimationSheet;

    private final ShapeConfig shapeConfig;

    private Skill(Builder b) {
        this.id = b.id;
        this.displayName = b.displayName;
        this.description = b.description;
        this.icon = b.icon;
        this.shape = b.shape;
        this.element = b.element;
        this.effects = Collections.unmodifiableList(new ArrayList<>(b.effects));
        this.cooldown = b.cooldown;
        this.vfxTexture = b.vfxTexture;
        this.vfxAnimation = b.vfxAnimation;
        this.vfxAnimationSheet = b.vfxAnimationSheet;
        this.shapeConfig = b.shapeConfig;
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
    public Texture      getVfxTexture()  { return vfxTexture; }
    public Animation<TextureRegion> getVfxAnimation() { return vfxAnimation; }
    public Texture      getVfxAnimationSheet() { return vfxAnimationSheet; }
    public ShapeConfig  getShapeConfig() { return shapeConfig; }

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
        private Texture vfxTexture;
        private Animation<TextureRegion> vfxAnimation;
        private Texture vfxAnimationSheet;
        private ShapeConfig shapeConfig;

        private Builder() {}

        public Builder id(String v)                                 { this.id = v; return this; }
        public Builder displayName(String v)                        { this.displayName = v; return this; }
        public Builder description(String v)                        { this.description = v; return this; }
        public Builder icon(Texture v)                              { this.icon = v; return this; }
        public Builder shape(Shape v)                               { this.shape = v; return this; }
        public Builder element(Element v)                           { this.element = v; return this; }
        public Builder cooldown(float v)                            { this.cooldown = v; this.cooldownSet = true; return this; }
        public Builder vfxTexture(Texture v)                        { this.vfxTexture = v; return this; }
        public Builder shapeConfig(ShapeConfig v)                   { this.shapeConfig = v; return this; }

        public Builder effect(Effect e)                             { this.effects.add(e); return this; }
        public Builder effects(List<Effect> e) {
            this.effects.clear();
            if (e != null) this.effects.addAll(e);
            return this;
        }

        /** Sets the animated VFX along with the spritesheet that backs it.
         *  The skill takes ownership of the spritesheet for disposal. */
        public Builder vfxAnimation(Animation<TextureRegion> anim, Texture spritesheet) {
            this.vfxAnimation = anim;
            this.vfxAnimationSheet = spritesheet;
            return this;
        }

        public Skill build() {
            require(id,          "id");
            require(displayName, "displayName");
            require(description, "description");
            require(icon,        "icon");
            require(shape,       "shape");
            require(element,     "element");
            require(vfxTexture,  "vfxTexture");
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
