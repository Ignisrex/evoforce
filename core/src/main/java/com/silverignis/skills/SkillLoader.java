package com.silverignis.skills;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.effects.EffectType;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.StatusType;

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

        Skill.Shape shape = parseEnum(Skill.Shape.class, requireString(node, id, "shape"), id, "shape");

        Skill.Builder b = Skill.builder()
            .id(id)
            .displayName(requireString(node, id, "displayName"))
            .description(requireString(node, id, "description"))
            .shape(shape)
            .element(parseEnum(Element.class, requireString(node, id, "element"), id, "element"))
            .cooldown(requireFloat(node, id, "cooldown"))
            .icon(loadTexture(requireString(node, id, "icon"), id, "icon"));

        b.powerScale(node.getFloat("powerScale", 0f));
        b.magicScale(node.getFloat("magicScale", 0f));
        b.manaCost(node.getInt("manaCost", 1));

        JsonValue effects = node.get("effects");
        if (effects != null) {
            b.effects(parseEffects(effects, id));
        }

        JsonValue shapeCfg = node.get("shapeConfig");
        if (shapeCfg != null) {
            b.shapeConfig(parseShapeConfig(shapeCfg, id, shape));
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
            EffectType type = parseEnum(EffectType.class, requireString(e, skillId, "effects[].type"), skillId, "effects[].type");

            switch (type){
                case DAMAGE:
                    effects.add(Effect.damage(requireInt(e, skillId, "effects[].value")));
                    break;
                case HEAL:
                    effects.add(Effect.heal(requireInt(e, skillId, "effects[].value")));
                    break;
                case APPLY_STATUS: {
                    StatusType st = parseEnum(StatusType.class, requireString(e, skillId, "effects[].statusType"), skillId, "effects[].statusType");
                    float duration = requireFloat(e, skillId, "effects[].duration");
                    int chance = e.getInt("chance", 100);
                    int dotMag = e.getInt("value", 0);
                    effects.add(Effect.applyStatus(st, duration, chance, dotMag));
                }break;
                case KNOCKBACK:
                    effects.add(Effect.knockback(requireInt(e, skillId, "effects[].value")));
                    break;
            }

        }
        return effects;
    }

    private static ShapeConfig parseShapeConfig(JsonValue node, String skillId, Skill.Shape shape) {
        switch (shape) {
            case PROJECTILE:
                return parseProjectileConfig(node, skillId);
            case STRIKE:
                return new StrikeConfig(
                    node.getInt("dashTiles", 1),
                    node.getInt("hitTiles", 1));
            case ZONE:
                return new ZoneConfig(
                    node.getBoolean("pull", false),
                    node.getFloat("duration", 1.0f),
                    node.getFloat("tickInterval", 0.33f));
            default:
                throw new IllegalStateException(
                    "Skill '" + skillId + "' has shapeConfig but shape " + shape + " does not support one");
        }
    }

    private static ShapeConfig parseProjectileConfig(JsonValue node, String skillId) {
        String movement = requireString(node, skillId, "shapeConfig.movementType");
        ProjectileConfig.MovementType type = parseEnum(
            ProjectileConfig.MovementType.class, movement, skillId, "shapeConfig.movementType");
        switch (type) {
            case STRAIGHT:
                return ProjectileConfig.straight(requireFloat(node, skillId, "shapeConfig.speed"));
            case LOB:
                return ProjectileConfig.lob(
                    requireInt(node, skillId, "shapeConfig.targetRange"),
                    requireFloat(node, skillId, "shapeConfig.arcHeight"),
                    node.getFloat("zoneDuration", 3.0f),
                    node.getFloat("zoneTickInterval", 0.5f));
            default:
                throw new IllegalStateException(
                    "Skill '" + skillId + "' has unsupported movementType '" + movement + "'");
        }
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
