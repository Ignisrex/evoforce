package com.silverignis.skills.visuals;

import com.silverignis.assets.GameAssets;

import java.util.Map;
import java.util.function.Supplier;

public final class SkillVisuals {

    private SkillVisuals() {}

    static GameAssets assets;
    public static void init(GameAssets a) { assets = a; }

    private static final Map<String, Supplier<SkillVisual>> CATALOG = Map.ofEntries(
      // per-skill visuals land here as each shape migrates
        //projectile skills
        Map.entry("dark_blast", DarkBlastVisual::new),
        Map.entry("fire_blast", FireBlastVisual::new),
        Map.entry("wind_slash", WindSlashVisual::new),
        //lob skills
        Map.entry("venom_bomb", VenomBombVisual::new),
        Map.entry("electro_ball", ElectroBallVisual::new),
        //beam skills
        Map.entry("ice_beam", IceBeamVisual::new),
        Map.entry("thunder", ThunderVisual::new),
        Map.entry("flame_torrent", FlameTorrentVisual::new),
        //strike skills
        Map.entry("wind_strike", WindStrikeVisual::new),
        Map.entry("flame_claw", FlameClawVisual::new),
        //zone skills
        Map.entry("frost_trap", FrostTrapVisual::new),
        Map.entry("void_pull", VoidPullVisual::new),
        //lob clouds — synthetic zones named <parent>_cloud
        Map.entry("venom_bomb_cloud", VenomBombCloudVisual::new),
        Map.entry("electro_ball_cloud", ElectroBallCloudVisual::new),
        //aura skills
        Map.entry("shield", ShieldVisual::new),
        Map.entry("heal", HealVisual::new),
        Map.entry("regen", RegenVisual::new),
        Map.entry("power_up", PowerUpVisual::new),
        Map.entry("magic_up", MagicUpVisual::new)
    );

    public static boolean has(String name) {return CATALOG.containsKey(name);}

    public static SkillVisual create(String name) {
        Supplier<SkillVisual> s = CATALOG.get(name);
        if ( s == null ) throw new IllegalArgumentException(
            "Unknown skill visual '" + name + "' (known: " + CATALOG.keySet() + ")");
        return s.get();
    }
}
