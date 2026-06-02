package com.silverignis.sessions;

import com.badlogic.gdx.utils.Disposable;

import com.silverignis.skills.SkillLibrary;
import com.silverignis.systems.SpawnSystem;
import com.silverignis.systems.spawn.SpawnTable;

public class GameSession implements Disposable {

    public final SkillLibrary skills;
    public final PlayerProfile playerProfile;
    public final SpawnTable spawnTable;
    public GameSession() {
        this.skills = SkillLibrary.defaults();
        this.playerProfile = new PlayerProfile(skills);
        this.spawnTable = SpawnTable.load();
    }

    @Override
    public void dispose() {
        skills.dispose();
    }
}
