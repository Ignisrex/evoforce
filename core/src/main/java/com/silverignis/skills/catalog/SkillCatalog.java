package com.silverignis.skills.catalog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.silverignis.skills.SkillLibrary;

public final class SkillCatalog {

    private SkillCatalog() {}

    public static SkillLibrary build() {
        SkillLibrary lib = new SkillLibrary();
        lib.add(WindStrike.define());
        lib.add(FireBlast.define());
        lib.add(VenomBomb.define());
        lib.add(IceBeam.define());
        lib.add(FrostTrap.define());
        lib.add(Heal.define());
        lib.add(Regen.define());
        lib.add(Shield.define());
        lib.add(WindSlash.define());
        lib.add(PowerUp.define());
        lib.add(MagicUp.define());
        lib.add(FlameClaw.define());
        lib.add(Thunder.define());
        lib.add(VoidPull.define());
        lib.add(DarkBlast.define());
        lib.add(FlameTorrent.define());
        lib.add(ElectroBall.define());
        lib.add(SporeDraught.define());
        lib.add(VerdantFang.define());
        lib.add(ToxicSpine.define());
        lib.add(ArcBolt.define());
        lib.add(UmbralWave.define());
        lib.add(VoltLance.define());
        lib.add(VoidCollapse.define());
        lib.add(MiasmaBeam.define());
        lib.add(SeedBomb.define());
        return lib;
    }

    static Texture icon(String path) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Could not load skill icon '" + path + "'", ex);
        }
    }
}
