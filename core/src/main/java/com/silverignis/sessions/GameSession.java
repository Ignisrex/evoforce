package com.silverignis.sessions;

import com.badlogic.gdx.utils.Disposable;
import com.silverignis.components.Caster;
import com.silverignis.components.Stats;
import com.silverignis.components.Team;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillLibrary;

public class GameSession implements Disposable {

    public final SkillLibrary skills;
    public final PlayerProfile playerProfile;

    public GameSession() {
        this.skills = SkillLibrary.defaults();
        this.playerProfile = new PlayerProfile(skills);
    }

    @Override
    public void dispose() {
        skills.dispose();
    }
}
