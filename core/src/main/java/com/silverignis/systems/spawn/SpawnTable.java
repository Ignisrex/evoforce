package com.silverignis.systems.spawn;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class SpawnTable {

    public enum Tier {
        TIER1("tier1"),
        TIER2("tier2"),
        TIER3("tier3"),
        TIER4("tier4"),
        TIER5("tier5");

        private final String jsonKey;
        Tier(String jsonKey) { this.jsonKey = jsonKey; }
        public String jsonKey() { return jsonKey; }
    }

    private final EnumMap<Tier, List<SpawnConfig>> configsByTier = new EnumMap<>(Tier.class);

    public SpawnTable() {}

    public void add(Tier tier, SpawnConfig config) {
        configsByTier.computeIfAbsent(tier, t -> new ArrayList<>()).add(config);
    }

    public List<SpawnConfig> get(Tier tier) {
        List<SpawnConfig> list = configsByTier.get(tier);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public static SpawnTable load() {
        return SpawnLoader.load(Gdx.files.internal("spawns.json"));
    }
}
