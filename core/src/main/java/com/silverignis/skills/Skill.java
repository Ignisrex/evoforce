package com.silverignis.skills;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;
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

    private final ShapeConfig shapeConfig;

    public Skill(String id,
                 String displayName,
                 String description,
                 Texture icon,
                 Shape shape,
                 Element element,
                 List<Effect> effects,
                 float cooldown,
                 Texture vfxTexture,
                 Animation<TextureRegion> vfxAnimation,
                 ShapeConfig shapeConfig) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.shape = shape;
        this.element = element;
        this.effects = effects == null
                ? Collections.<Effect>emptyList()
                : Collections.unmodifiableList(effects);
        this.cooldown = cooldown;
        this.vfxTexture = vfxTexture;
        this.vfxAnimation = vfxAnimation;
        this.shapeConfig = shapeConfig;
    }

    public Skill(String id,
                 String displayName,
                 String description,
                 Texture icon,
                 Shape shape,
                 Element element,
                 List<Effect> effects,
                 float cooldown,
                 Texture vfxTexture,
                 ShapeConfig shapeConfig) {
        this(id, displayName, description, icon, shape, element, effects, cooldown, vfxTexture, null, shapeConfig);
    }

    public Skill(String id,
                 String displayName,
                 String description,
                 Texture icon,
                 Shape shape,
                 Element element,
                 List<Effect> effects,
                 float cooldown,
                 Texture vfxTexture) {
        this(id, displayName, description, icon, shape, element, effects, cooldown, vfxTexture, null, null);
    }

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
    public ShapeConfig   getShapeConfig() { return shapeConfig; }
}
