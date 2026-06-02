package com.silverignis.systems.spawn;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.silverignis.registry.Monster;

import java.util.ArrayList;
import java.util.List;

public final class SpawnLoader {

    private SpawnLoader() {}

    public static SpawnTable load(FileHandle file) {
        JsonValue root = new JsonReader().parse(file);
        if (root == null) {
            throw new IllegalStateException("Spawn file '" + file.path() + "' is empty or unreadable");
        }

        SpawnTable spawnTable = new SpawnTable();
        for (SpawnTable.Tier tier : SpawnTable.Tier.values()) {
            JsonValue tierEntry = root.get(tier.jsonKey());
            if (tierEntry == null) {
                throw new IllegalStateException(
                    "Spawn file '" + file.path() + "' missing tier '" + tier.jsonKey() + "'");
            }
            for (JsonValue entry = tierEntry.child; entry != null; entry = entry.next) {
                spawnTable.add(tier, parseSpawnConfig(entry, tier));
            }
        }

        return spawnTable;
    }

    private static SpawnConfig parseSpawnConfig(JsonValue node, SpawnTable.Tier tier) {
        String speciesName = requireString(node, tier, "species");
        Monster species = Monster.fromName(speciesName);
        if (species == null) {
            throw new IllegalStateException(
                "Spawn entry in '" + tier.jsonKey() + "' has unknown species '" + speciesName + "'");
        }

        List<String> skills = new ArrayList<>();
        JsonValue skillsArr = node.get("skills");
        if (skillsArr != null) {
            if (!skillsArr.isArray()) {
                throw new IllegalStateException(
                    "Spawn '" + speciesName + "' field 'skills' must be a JSON array");
            }
            for (JsonValue s = skillsArr.child; s != null; s = s.next) {
                skills.add(s.asString());
            }
        }

        String basicAttack = node.getString("basicAttack", null);

        JsonValue stats = node.get("stats");
        if (stats == null) {
            throw new IllegalStateException("Spawn '" + speciesName + "' missing required field 'stats'");
        }

        return SpawnConfig.builder()
            .species(species)
            .skillSet(skills)
            .basicAttack(basicAttack)
            .power(requireInt(stats, speciesName, "stats.power"))
            .magic(requireInt(stats, speciesName, "stats.magic"))
            .vitality(requireInt(stats, speciesName, "stats.vitality"))
            .defense(requireInt(stats, speciesName, "stats.defense"))
            .speed(requireInt(stats, speciesName, "stats.speed"))
            .build();
    }

    private static String requireString(JsonValue node, SpawnTable.Tier tier, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            throw new IllegalStateException(
                "Spawn entry in '" + tier.jsonKey() + "' missing required field '" + field + "'");
        }
        return node.getString(field);
    }

    private static int requireInt(JsonValue node, String speciesName, String field) {
        String leaf = field.substring(field.lastIndexOf('.') + 1);
        if (!node.has(leaf) || node.get(leaf).isNull()) {
            throw new IllegalStateException(
                "Spawn '" + speciesName + "' missing required field '" + field + "'");
        }
        return node.getInt(leaf);
    }
}
