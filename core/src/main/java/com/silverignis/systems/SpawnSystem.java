package com.silverignis.systems;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.silverignis.components.Stats;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.Enemy;
import com.silverignis.registry.MonsterRegistry;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillLibrary;
import com.silverignis.systems.spawn.SpawnConfig;
import com.silverignis.systems.spawn.SpawnTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SpawnSystem {

    private static final int MAX_BUDGET = 10;
    private static final int MAX_ENEMIES = 3;
    private static final int TIER_COUNT = SpawnTable.Tier.values().length;

    private final SpawnTable spawnTable;
    private final MonsterRegistry registry;
    private final SkillLibrary skillLibrary;
    private final Random rng;

    private final Map<Integer, List<int[]>> partitionsByBudget = new HashMap<>();

    public SpawnSystem(SpawnTable spawnTable, MonsterRegistry registry, SkillLibrary skillLibrary) {
        this(spawnTable, registry, skillLibrary, new Random());
    }

    public SpawnSystem(SpawnTable spawnTable, MonsterRegistry registry, SkillLibrary skillLibrary, Random rng) {
        this.spawnTable = spawnTable;
        this.registry = registry;
        this.skillLibrary = skillLibrary;
        this.rng = rng;
    }

    public List<Enemy> spawnNext(Battlefield battlefield, int progressionLevel) {
        int budget = Math.min(Math.max(progressionLevel, 0) + 1, MAX_BUDGET);
        List<int[]> partitions = partitionsByBudget.computeIfAbsent(budget, this::buildPartitions);
        if (partitions.isEmpty()) {
            throw new IllegalStateException("No spawn partition fits budget " + budget + " under MAX_ENEMIES=" + MAX_ENEMIES);
        }

        int[] chosen = partitions.get(rng.nextInt(partitions.size()));

        List<SpawnConfig> drawn = new ArrayList<>();
        for (int t = 0; t < TIER_COUNT; t++) {
            int count = chosen[t];
            if (count == 0) continue;
            SpawnTable.Tier tier = SpawnTable.Tier.values()[t];
            List<SpawnConfig> pool = spawnTable.get(tier);
            if (pool.isEmpty()) {
                throw new IllegalStateException("SpawnTable has no entries for tier " + tier);
            }
            for (int i = 0; i < count; i++) {
                drawn.add(pool.get(rng.nextInt(pool.size())));
            }
        }

        return buildEnemies(drawn, battlefield);
    }

    private List<int[]> buildPartitions(int budget) {
        List<int[]> all = new ArrayList<>();
        enumerate(budget, TIER_COUNT, new int[TIER_COUNT], all);
        List<int[]> filtered = new ArrayList<>();
        for (int[] p : all) {
            int total = 0;
            for (int v : p) total += v;
            if (total > 0 && total <= MAX_ENEMIES) filtered.add(p);
        }
        return filtered;
    }

    private void enumerate(int remaining, int maxTier, int[] current, List<int[]> out) {
        if (remaining == 0) {
            out.add(current.clone());
            return;
        }
        if (maxTier == 0) return;
        int cost = maxTier;
        int idx = maxTier - 1;
        int maxCount = remaining / cost;
        for (int c = 0; c <= maxCount; c++) {
            current[idx] = c;
            enumerate(remaining - c * cost, maxTier - 1, current, out);
        }
        current[idx] = 0;
    }

    private List<Enemy> buildEnemies(List<SpawnConfig> configs, Battlefield battlefield) {
        List<Enemy> result = new ArrayList<>(configs.size());
        int col = Battlefield.COLS - 1;
        int row = 0;
        for (SpawnConfig cfg : configs) {
            Sprite sprite = new Sprite(registry.getMonsterTexture(cfg.species, Team.ENEMY));
            Stats stats = new Stats(cfg.power, cfg.magic, cfg.vitality, cfg.defense, cfg.speed);
            Enemy enemy = new Enemy(col, row, sprite, battlefield, stats);
            enemy.setupSkills(resolveSkills(cfg), resolveBasicAttack(cfg));
            result.add(enemy);

            row++;
            if (row >= Battlefield.ROWS) {
                row = 0;
                col--;
            }
        }
        return result;
    }

    private List<Skill> resolveSkills(SpawnConfig cfg) {
        List<Skill> skills = new ArrayList<>(cfg.skillSet.size());
        for (String id : cfg.skillSet) {
            Skill s = skillLibrary.get(id);
            if (s == null) {
                throw new IllegalStateException(
                    "Spawn '" + cfg.species + "' references unknown skill id '" + id + "'");
            }
            skills.add(s);
        }
        return skills;
    }

    private Skill resolveBasicAttack(SpawnConfig cfg) {
        if (cfg.basicAttack == null) return null;
        Skill s = skillLibrary.get(cfg.basicAttack);
        if (s == null) {
            throw new IllegalStateException(
                "Spawn '" + cfg.species + "' references unknown basicAttack '" + cfg.basicAttack + "'");
        }
        return s;
    }
}
