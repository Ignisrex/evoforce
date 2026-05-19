package com.silverignis.skills;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.elements.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads skill definitions from a JSON file and produces a populated
 * {@link SkillLibrary}. Throws {@link IllegalStateException} naming the
 * offending skill id and field if anything required is missing or malformed.
 */
public final class SkillLoader {

    private SkillLoader() {}

    public static SkillLibrary load(FileHandle file) {
        JsonValue root = new JsonReader().parse(file);
        if (root == null || !root.isArray()) {
            throw new IllegalStateException("Skill file '" + file.path() + "' must be a JSON array at the top level");
        }

        SkillLibrary lib = new SkillLibrary();
        for (JsonValue entry = root.child; entry != null; entry = entry.next) {
            lib.add(parseSkill(entry));
        }
        return lib;
    }

    private static Skill parseSkill(JsonValue node) {
        String id = node.getString("id", null);
        if (id == null) throw missing("<unknown>", "id");

        Skill.Builder b = Skill.builder()
            .id(id)
            .displayName(requireString(node, id, "displayName"))
            .description(requireString(node, id, "description"))
            .shape(parseEnum(Skill.Shape.class, requireString(node, id, "shape"), id, "shape"))
            .element(parseEnum(Element.class, requireString(node, id, "element"), id, "element"))
            .cooldown(requireFloat(node, id, "cooldown"))
            .icon(loadTexture(requireString(node, id, "icon"), id, "icon"))
            .vfxTexture(loadTexture(requireString(node, id, "vfxTexture"), id, "vfxTexture"));

        JsonValue effects = node.get("effects");
        if (effects != null) {
            b.effects(parseEffects(effects, id));
        }

        JsonValue anim = node.get("vfxAnimation");
        if (anim != null) {
            Texture sheet = loadTexture(requireString(anim, id, "vfxAnimation.spritesheet"),
                                        id, "vfxAnimation.spritesheet");
            int frameW = requireInt(anim, id, "vfxAnimation.frameWidth");
            int frameH = requireInt(anim, id, "vfxAnimation.frameHeight");
            float frameDuration = requireFloat(anim, id, "vfxAnimation.frameDuration");
            b.vfxAnimation(buildAnimation(sheet, frameW, frameH, frameDuration), sheet);
        }

        JsonValue shapeCfg = node.get("shapeConfig");
        if (shapeCfg != null) {
            b.shapeConfig(parseShapeConfig(shapeCfg, id));
        }

        return b.build();
    }

    private static List<Effect> parseEffects(JsonValue arr, String skillId) {
        if (!arr.isArray()) {
            throw new IllegalStateException(
                "Skill '" + skillId + "' field 'effects' must be a JSON array");
        }
        List<Effect> effects = new ArrayList<>();
        for (JsonValue e = arr.child; e != null; e = e.next) {
            Effect.Type type = parseEnum(Effect.Type.class, requireString(e, skillId, "effects[].type"), skillId, "effects[].type");
            int value    = e.getInt("value", 0);
            int duration = e.getInt("duration", 0);
            int chance   = e.getInt("chance", 100);
            effects.add(new Effect(type, value, duration, chance));
        }
        return effects;
    }

    private static ShapeConfig parseShapeConfig(JsonValue node, String skillId) {
        String movement = requireString(node, skillId, "shapeConfig.movementType");
        ProjectileConfig.MovementType type = parseEnum(
            ProjectileConfig.MovementType.class, movement, skillId, "shapeConfig.movementType");
        switch (type) {
            case STRAIGHT:
                return ProjectileConfig.straight(requireFloat(node, skillId, "shapeConfig.speed"));
            case LOB:
                return ProjectileConfig.lob(
                    requireInt(node, skillId, "shapeConfig.targetRange"),
                    requireFloat(node, skillId, "shapeConfig.arcHeight"));
            default:
                throw new IllegalStateException(
                    "Skill '" + skillId + "' has unsupported movementType '" + movement + "'");
        }
    }

    private static Animation<TextureRegion> buildAnimation(Texture sheet, int frameW, int frameH,
                                                           float frameDuration) {
        int cols = sheet.getWidth() / frameW;
        TextureRegion[][] grid = TextureRegion.split(sheet, frameW, frameH);
        TextureRegion[] frames = new TextureRegion[cols];
        for (int i = 0; i < cols; i++) frames[i] = grid[0][i];
        return new Animation<>(frameDuration, frames);
    }

    private static Texture loadTexture(String path, String skillId, String field) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                "Skill '" + skillId + "' field '" + field + "' could not load texture '" + path + "'",
                ex);
        }
    }

    private static String requireString(JsonValue node, String skillId, String field) {
        String key = leafKey(field);
        if (!node.has(key) || node.get(key).isNull()) throw missing(skillId, field);
        return node.getString(key);
    }

    private static int requireInt(JsonValue node, String skillId, String field) {
        String key = leafKey(field);
        if (!node.has(key) || node.get(key).isNull()) throw missing(skillId, field);
        return node.getInt(key);
    }

    private static float requireFloat(JsonValue node, String skillId, String field) {
        String key = leafKey(field);
        if (!node.has(key) || node.get(key).isNull()) throw missing(skillId, field);
        return node.getFloat(key);
    }

    private static String leafKey(String dottedField) {
        int dot = dottedField.lastIndexOf('.');
        return dot < 0 ? dottedField : dottedField.substring(dot + 1);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> cls, String value, String skillId, String field) {
        try {
            return Enum.valueOf(cls, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                "Skill '" + skillId + "' field '" + field + "' has invalid "
                    + cls.getSimpleName() + " '" + value + "'", ex);
        }
    }

    private static IllegalStateException missing(String skillId, String field) {
        return new IllegalStateException("Skill '" + skillId + "' missing required field '" + field + "'");
    }
}
