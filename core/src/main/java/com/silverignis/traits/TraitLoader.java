package com.silverignis.traits;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Reads trait definitions from a JSON file and produces a populated
 * {@link TraitLibrary}. JSON carries presentation only (id, displayName,
 * description, icon) plus the name of a Java-defined {@link TraitEffect};
 * behavior lives in code. Throws {@link IllegalStateException} naming the
 * offending trait id and field if anything is missing or malformed.
 */
public final class TraitLoader {

    private TraitLoader() {}

    public static TraitLibrary load(FileHandle file) {
        JsonValue root = new JsonReader().parse(file);
        if (root == null || !root.isArray()) {
            throw new IllegalStateException("Trait file '" + file.path() + "' must be a JSON array at the top level");
        }

        TraitLibrary lib = new TraitLibrary();
        for (JsonValue entry = root.child; entry != null; entry = entry.next) {
            lib.add(parseTrait(entry));
        }
        return lib;
    }

    private static Trait parseTrait(JsonValue node) {
        String id = node.getString("id", null);
        if (id == null) throw missing("<unknown>", "id");

        return new Trait(
            id,
            requireString(node, id, "displayName"),
            requireString(node, id, "description"),
            loadTexture(requireString(node, id, "icon"), id, "icon"),
            parseEnum(TraitEffect.class, requireString(node, id, "effect"), id, "effect"));
    }

    private static Texture loadTexture(String path, String traitId, String field) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                "Trait '" + traitId + "' field '" + field + "' could not load texture '" + path + "'", ex);
        }
    }

    private static String requireString(JsonValue node, String traitId, String field) {
        if (!node.has(field) || node.get(field).isNull()) throw missing(traitId, field);
        return node.getString(field);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> cls, String value, String traitId, String field) {
        try {
            return Enum.valueOf(cls, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                "Trait '" + traitId + "' field '" + field + "' has invalid "
                    + cls.getSimpleName() + " '" + value + "'", ex);
        }
    }

    private static IllegalStateException missing(String traitId, String field) {
        return new IllegalStateException("Trait '" + traitId + "' missing required field '" + field + "'");
    }
}
